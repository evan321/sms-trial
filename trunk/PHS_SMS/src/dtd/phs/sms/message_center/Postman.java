package dtd.phs.sms.message_center;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import dtd.phs.sms.data.entities.MessageItem;
import dtd.phs.sms.data.entities.SMSItem;

//TODO: All this must be called inside an service + new thread
public class Postman 
implements 
ISMS_SendListener,
INormalMessageSenderListener
{

	public static final String GENERAL_BEING_SENT_EVENT = "dtd.phs.sms.sms_being_sent";
	private SendMessageListener messageListener;
	private ISMSSender iSender;
	private INormalMessageSender sender;
	private Context context;

	public Postman(Context context) {
		this.context = context;
		iSender = new GoogleSender(this, context);
		sender = new AndroidSMSSender(this,context);
	}

	public void sendMessage(MessageItem message, SendMessageListener listener, boolean forceSendNormalSMS) {
		//TODO: ping the receiver (friend) before send "real" message, if the friend is connected then:
		// let forceSendNormalSMS = false - The connectivity will be checked again inside the sender,
		// but it decrease the chance user have to wait too long to send a "normal" message to a friend
		// which doesn't use G-Message
		saveSentMessage( message );
		if ( ! forceSendNormalSMS ) {
			setListener(listener);
			tryToSendIMessage(message);
		} else {
			tryToSendNormalMessage(message);
		}
	}

	/**
	 * Message is sent, but later there could be an error or delivered, whatever 
	 * @param message
	 */
	private void saveSentMessage(MessageItem message) {
		ContentValues values = new ContentValues();
		values.put(SMSItem.ADDRESS, message.getNumber());
		values.put(SMSItem.BODY, message.getContent());
		context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values );
		broadcastMessageIsBeingSent();
	}

	private void broadcastMessageIsBeingSent() {
		Intent i = new Intent();
		i.setAction(GENERAL_BEING_SENT_EVENT);
		context.sendBroadcast(i);
	}

	private void tryToSendIMessage(MessageItem message) {
		
		iSender.send( message );
	}
	private void setListener(SendMessageListener listener) {
		this.messageListener = listener;
	}

	private void tryToSendNormalMessage(MessageItem mess) {
		
		sender.send( mess );
	}
	/**
	 * I-MESSAGE - Interface: ISMS_SendListener
	 * BEGIN
	 */
	@Override
	public void onSendIMessageSuccess(Object data) {
		messageListener.onSendSuccces(data);
	}

	@Override
	public void onSendIMessageFailed(Object data) {
		if ( data instanceof MessageItem ) {
			MessageItem mess = (MessageItem) data;
			tryToSendNormalMessage(mess);
		}
	}
	
	/**
	 * END
	 * I-MESSAGE - Interface: ISMS_SendListener
	 */

	
	@Override
	public void onNormalMessageSendFailed(Object data) {
		messageListener.onSendSuccces(data);
	}

	@Override
	public void onNormalMessageSendSuccess(Object data) {
		messageListener.onSendFailed( data );
	}

	public void startInternetPostman() {
		iSender.startInternetPostmanService(context);
	}

	public static String getServiceDomain() {
		return "@"+GoogleXMPPService.SERVICE;
	}


}
