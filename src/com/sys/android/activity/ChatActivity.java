package com.sys.android.activity;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.app.Activity;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sys.android.activity.adapter.ChatListAdapter;
import com.sys.android.entity.MessageInfo;
import com.sys.android.selfview.RecordButton;
import com.sys.android.selfview.RecordButton.OnFinishedRecordListener;
import com.sys.android.util.TimeRender;
import com.sys.android.xmpp.R;
import com.sys.android.xmppmanager.XmppConnection;

public class ChatActivity extends Activity {

	private String userChat = "";// ��ǰ���� userChat
	private String userChatSendFile = "";// ��˭���ļ�
	private ChatListAdapter adapter;
	private List<MessageInfo> listMsg = new LinkedList<MessageInfo>();
	private String pUSERID;// �Լ���user
	private String pFRIENDID;// ���ڵ� ����
	private EditText msgText;
	private TextView chat_name;
	private NotificationManager mNotificationManager;
	private ChatManager cm;
	private RecordButton mRecordButton;

	// �����ļ�
	private OutgoingFileTransfer sendTransfer;
	public static String FILE_ROOT_PATH = Environment
			.getExternalStorageDirectory().getPath() + "/chat/file";
	public static String RECORD_ROOT_PATH = Environment
			.getExternalStorageDirectory().getPath() + "/chat/record";
	Chat newchat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chat_client);

		init();

		mRecordButton = (RecordButton) findViewById(R.id.record_button);

		String path = RECORD_ROOT_PATH;
		File file = new File(path);
		file.mkdirs();
		path += "/" + System.currentTimeMillis() + ".amr";
		mRecordButton.setSavePath(path);
		mRecordButton
				.setOnFinishedRecordListener(new OnFinishedRecordListener() {

					@Override
					public void onFinishedRecord(String audioPath, int time) {
						Log.i("RECORD!!!", "finished!!!!!!!!!! save to "
								+ audioPath);

						if (audioPath != null) {
							try {
								// �Լ���ʾ��Ϣ
								MessageInfo myChatMsg = new MessageInfo(pUSERID,
										time + "��������Ϣ", TimeRender.getDate(),
										MessageInfo.FROM_TYPE[1], MessageInfo.TYPE[0],
										MessageInfo.STATUS[3], time + "", audioPath);
								listMsg.add(myChatMsg);
								String[] pathStrings = audioPath.split("/"); // �ļ���

								// ���� �Է�����Ϣ
								String fileName = null;
								if (pathStrings != null
										&& pathStrings.length > 0) {
									fileName = pathStrings[pathStrings.length - 1];
								}
								MessageInfo sendChatMsg = new MessageInfo(pUSERID, time
										+ "��������Ϣ", TimeRender.getDate(),
										MessageInfo.FROM_TYPE[0], MessageInfo.TYPE[0],
										MessageInfo.STATUS[3], time + "", fileName);

								// ˢ��������
								adapter.notifyDataSetChanged();

								// ������Ϣ
								newchat.sendMessage(MessageInfo.toJson(sendChatMsg));
								sendFile(audioPath, myChatMsg);//
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							Toast.makeText(ChatActivity.this, "����ʧ��",
									Toast.LENGTH_SHORT).show();
						}

					}
				});

	}

	private void init() {
		mNotificationManager = (NotificationManager) this
				.getSystemService(Service.NOTIFICATION_SERVICE);
		// ��ȡIntent���������û���
		this.pUSERID = getIntent().getStringExtra("USERID");
		this.userChat = getIntent().getStringExtra("user");/*
															 * + "/" +
															 * FriendListActivity
															 * .RESOUCE_NAME;
															 */
		userChatSendFile = userChat + "/" + FriendListActivity.MY_RESOUCE_NAME;
		this.pFRIENDID = getIntent().getStringExtra("FRIENDID");
		/*
		 * System.out.println("������Ϣ���û�pFRIENDID�ǣ�" + userChat);
		 * System.out.println("������Ϣ���û�pUSERID�ǣ�" + pUSERID);
		 * System.out.println(" ��Ϣ���û�pFRIENDID�ǣ�" + pFRIENDID);
		 */

		chat_name = (TextView) findViewById(R.id.chat_name);
		chat_name.setText(pFRIENDID);
		ListView listview = (ListView) findViewById(R.id.formclient_listview);
		listview.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		this.adapter = new ChatListAdapter(this, listMsg);
		listview.setAdapter(adapter);
		// ��ȡ�ı���Ϣ
		this.msgText = (EditText) findViewById(R.id.formclient_text);
		// ��Ϣ����
		cm = XmppConnection.getConnection().getChatManager();

		// ���ذ�ť
		Button mBtnBack = (Button) findViewById(R.id.chat_back);
		mBtnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});

//		receivedMsg();// ������Ϣ
		textMsg();// ����+������Ϣ
		receivedFile();// �����ļ�

	}

	/**
	 * ������Ϣ
	 */
	public void receivedMsg() {

		cm.addChatListener(new ChatManagerListener() {
			@Override
			public void chatCreated(Chat chat, boolean able) {
				chat.addMessageListener(new MessageListener() {
					@Override
					public void processMessage(Chat chat2, Message message) {
						// �յ�����pc����������Ϣ����ȡ�Լ����ѷ�������Ϣ��
						if (message.getFrom().contains(userChat)) {
							// Msg.analyseMsgBody(message.getBody(),userChat);
							// ��ȡ�û�����Ϣ��ʱ�䡢IN
							/*
							 * String[] args = new String[] { userChat,
							 * message.getBody(), TimeRender.getDate(), "IN" };
							 */
							// ��handler��ȡ������ʾ��Ϣ
							android.os.Message msg = handler.obtainMessage();
							System.out.println("��������������Ϣ�� chat��"
									+ message.getBody());
							msg.what = 1;
							msg.obj = message.getBody();
							msg.sendToTarget();

						}
					}
				});
			}
		});
	}

	/**
	 * ������Ϣ
	 * 
	 * @author Administrator
	 * 
	 */
	public void textMsg() {
		// ������Ϣ
		Button btsend = (Button) findViewById(R.id.formclient_btsend);
		// ������Ϣ��pc�������ĺ��ѣ���ȡ�Լ��ķ��������ͺ��ѣ�
		//TODO:Ӧ�ȼ���Ƿ���δ����Ϣ
		newchat = cm.createChat(userChat, new MessageListener() {
		    public void processMessage(Chat chat, Message message) {
		        System.out.println("Received message: " + message);
				MessageInfo chatMsg = new MessageInfo(pUSERID, message.getBody(), TimeRender.getDate(),
						MessageInfo.FROM_TYPE[0]); 
				listMsg.add(chatMsg);
				// ˢ�������� ��UI�̲߳���ֱ�Ӹ���
					ChatActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							adapter.notifyDataSetChanged();						
						}
					});
		    }
		});


		btsend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// ��ȡtext�ı�
				final String msg = msgText.getText().toString();
				if (msg.length() > 0) {
					// �Լ���ʾ��Ϣ
					MessageInfo chatMsg = new MessageInfo(pUSERID, msg, TimeRender.getDate(),
							MessageInfo.FROM_TYPE[1]);
					listMsg.add(chatMsg);
					// ˢ��������
					adapter.notifyDataSetChanged();
					try {
						// ������Ϣ
						Message toSend = new Message();
						toSend.setBody(msg);
						newchat.sendMessage(toSend);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					Toast.makeText(ChatActivity.this, "������Ϣ����Ϊ��",
							Toast.LENGTH_SHORT).show();
				}
				// ���text
				msgText.setText("");
			}
		});
	}

	/**
	 * �����ļ�
	 * 
	 * @author Administrator
	 * 
	 */
	public void receivedFile() {
		/**
		 * �����ļ�
		 */
		// Create the file transfer manager
		final FileTransferManager manager = new FileTransferManager(
				XmppConnection.getConnection());
		// Create the listener
		manager.addFileTransferListener(new FileTransferListener() {

			public void fileTransferRequest(FileTransferRequest request) {
				// Check to see if the request should be accepted
				Log.d("receivedFile ", " receive file");
				if (shouldAccept(request)) {
					// Accept it
					IncomingFileTransfer transfer = request.accept();
					try {

						System.out.println(request.getFileName());
						File file = new File(RECORD_ROOT_PATH
								+ request.getFileName());

						android.os.Message msg = handler.obtainMessage();
						transfer.recieveFile(file);
						MessageInfo msgInfo = queryMsgForListMsg(file.getName());
						msgInfo.setFilePath(file.getPath());// ���� filepath
						new MyFileStatusThread(transfer, msgInfo).start();

					} catch (XMPPException e) {
						e.printStackTrace();
					}
				} else {
					// Reject it
					request.reject();
					String[] args = new String[] { userChat,
							request.getFileName(), TimeRender.getDate(), "IN",
							MessageInfo.TYPE[0], MessageInfo.STATUS[1] };
					MessageInfo msgInfo = new MessageInfo(args[0], "redio", args[2], args[3],
							MessageInfo.TYPE[0], MessageInfo.STATUS[1]);
					// ��handler��ȡ������ʾ��Ϣ
					android.os.Message msg = handler.obtainMessage();
					msg.what = 5;
					msg.obj = msgInfo;
					handler.sendMessage(msg);
				}
			}
		});
	}

	/**
	 * �����ļ�
	 * 
	 * @param path
	 */
	public void sendFile(String path, MessageInfo msg) {
		/**
		 * �����ļ�
		 */
		// Create the file transfer manager
		FileTransferManager sendFilemanager = new FileTransferManager(
				XmppConnection.getConnection());

		// Create the outgoing file transfer
		sendTransfer = sendFilemanager
				.createOutgoingFileTransfer(userChatSendFile);
		// Send the file
		try {

			sendTransfer.sendFile(new java.io.File(path), "send file");
			new MyFileStatusThread(sendTransfer, msg).start();
			/**
			 * ����
			 */
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

	class MyFileStatusThread extends Thread {
		private FileTransfer transfer;
		private MessageInfo msg;

		public MyFileStatusThread(FileTransfer tf, MessageInfo msg) {
			transfer = tf;
			this.msg = msg;
		}

		public void run() {
			System.out.println(transfer.getStatus());
			System.out.println(transfer.getProgress());
			android.os.Message message = new android.os.Message();// handle
			message.what = 3;
			while (!transfer.isDone()) {
				System.out.println(transfer.getStatus());
				System.out.println(transfer.getProgress());

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			if (transfer.getStatus().equals(Status.error)) {
				msg.setReceive(MessageInfo.STATUS[2]);
			} else if (transfer.getStatus().equals(Status.refused)) {
				msg.setReceive(MessageInfo.STATUS[1]);
			} else {
				msg.setReceive(MessageInfo.STATUS[0]);// �ɹ�

			}

			handler.sendMessage(message);
			/*
			 * System.out.println(transfer.getStatus());
			 * System.out.println(transfer.getProgress());
			 */
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				MessageInfo chatMsg = MessageInfo.analyseMsgBody(msg.obj.toString());
				if (chatMsg != null) {
					listMsg.add(chatMsg);// ��ӵ�������Ϣ
					adapter.notifyDataSetChanged();
				}

				break;
			case 2: // �����ļ�

				break;
			case 3: // �����ļ�����״̬
				adapter.notifyDataSetChanged();
				break;
			case 5: // �����ļ�
				MessageInfo msg2 = (MessageInfo) msg.obj;
				System.out.println(msg2.getFrom());
				listMsg.add(msg2);
				adapter.notifyDataSetChanged();
			default:
				break;
			}
		};
	};

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// XmppConnection.closeConnection();
		System.exit(0);
	}

	protected void setNotiType(int iconId, String s) {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent appIntent = PendingIntent.getActivity(this, 0, intent, 0);
		Notification myNoti = new Notification();
		myNoti.icon = iconId;
		myNoti.tickerText = s;
		myNoti.defaults = Notification.DEFAULT_SOUND;
		myNoti.flags |= Notification.FLAG_AUTO_CANCEL;
		myNoti.setLatestEventInfo(this, "QQ��Ϣ", s, appIntent);
		mNotificationManager.notify(0, myNoti);
	}

	/**
	 * �Ƿ����
	 * 
	 * @param request
	 * @return
	 */
	private boolean shouldAccept(FileTransferRequest request) {
		final boolean isAccept[] = new boolean[1];

		return true;
	}

	protected void dialog() {

	}

	/**
	 * init file
	 */
	static {
		File root = new File(FILE_ROOT_PATH);
		root.mkdirs();// û�и�Ŀ¼������Ŀ¼
		root = new File(RECORD_ROOT_PATH);
		root.mkdirs();
	}

	/**
	 * ��list ��ȡ�� �ּ�������ͬ�� Msg
	 */
	private MessageInfo queryMsgForListMsg(String filePath) {

		MessageInfo msg = null;
		for (int i = listMsg.size() - 1; i >= 0; i--) {
			msg = listMsg.get(i);
			if (filePath != null && filePath.contains(msg.getFilePath())) {// �Է���������ֻ���ļ�������
				return msg;
			}
		}
		return msg;
	}
}