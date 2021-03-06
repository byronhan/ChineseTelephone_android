package com.richitec.chinesetelephone.call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.sip.SipUtils;
import com.richitec.chinesetelephone.sip.listeners.SipInviteStateListener;
import com.richitec.chinesetelephone.sip.services.BaseSipServices;

public class OutgoingCallActivity extends Activity implements
		SipInviteStateListener {

	private static final String LOG_TAG = "OutgoingCallActivity";

	// sip services
	private static BaseSipServices _smSipServices;

	// outgoing call activity onCreate param key
	public static final String OUTGOING_CALL_PHONE = "outgoing_call_phone";
	public static final String OUTGOING_CALL_OWNERSHIP = "outgoing_call_ownership";

	// outgoing call phone number
	private String _mCalleePhone;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// keep outgoing call activity screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// set content view
		setContentView(R.layout.outgoing_call_activity_layout);

		// get the intent parameter data
		Bundle _data = getIntent().getExtras();

		// check the data bundle and get call phone
		if (null != _data) {
			// init outgoing call phone and set callee textView text
			if (null != _data.getString(OUTGOING_CALL_PHONE)) {
				_mCalleePhone = _data.getString(OUTGOING_CALL_PHONE);

				((TextView) findViewById(R.id.callee_textView))
						.setText(null != _data
								.getString(OUTGOING_CALL_OWNERSHIP) ? _data
								.getString(OUTGOING_CALL_OWNERSHIP)
								: _mCalleePhone);
			}

			// check sip services
			if (null != _smSipServices) {
				// set sip services sip invite state listener
				_smSipServices.setSipInviteStateListener(this);
			} else {
				Log.e(LOG_TAG, "Get sip services error, sip services is null");
			}
		}

		// set wallpaper as outgoing call background
		((ImageView) findViewById(R.id.outgoingcall_background_imageView))
				.setImageDrawable(getWallpaper());

		// get call controller gridView
		GridView _callControllerGridView = (GridView) findViewById(R.id.callController_gridView);

		// set call controller gridView adapter
		_callControllerGridView.setAdapter(generateCallControllerAdapter());

		// set call controller gridView on item click listener
		_callControllerGridView
				.setOnItemClickListener(new CallControllerGridViewOnItemClickListener());

		// bind hangup outgoing call button on click listener
		((ImageButton) findViewById(R.id.hangup_button))
				.setOnClickListener(new HangupOutgoingCallBtnOnClickListener());

		// bind hide keyboard button on click listener
		((ImageButton) findViewById(R.id.hideKeyboard_button))
				.setOnClickListener(new HideKeyboardBtnOnClickListener());
	}

	@Override
	public void onBackPressed() {
		// nothing to do
	}

	@Override
	public void onCallInitializing() {
		// update call state textView text
		((TextView) findViewById(R.id.callState_textView))
				.setText(R.string.outgoing_call_trying);
	}

	@Override
	public void onCallEarlyMedia() {
		// update call state textView text
		((TextView) findViewById(R.id.callState_textView))
				.setText(R.string.outgoing_call_earlyMedia7RemoteRing);
	}

	@Override
	public void onCallRemoteRinging() {
		// update call state textView text
		((TextView) findViewById(R.id.callState_textView))
				.setText(R.string.outgoing_call_earlyMedia7RemoteRing);
	}

	@Override
	public void onCallSpeaking() {
		// update call state textView text
		((TextView) findViewById(R.id.callState_textView)).setText("speaking");
	}

	@Override
	public void onCallFailed() {
		// update call state textView text
		((TextView) findViewById(R.id.callState_textView))
				.setText(R.string.outgoing_call_failed);

		// sip voice call terminated
		onCallTerminated();
	}

	@Override
	public void onCallTerminating() {
		// update call state textView text
		((TextView) findViewById(R.id.callState_textView))
				.setText(R.string.end_outgoing_call);
	}

	@Override
	public void onCallTerminated() {
		// delayed one second to back
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				// update call state textView text
				((TextView) findViewById(R.id.callState_textView))
						.setText(R.string.outgoing_call_ended);

				// finish outgoing call activity
				finish();
			}
		}, 1000);
	}

	// init outgoing call activity sip services
	public static void initSipServices(BaseSipServices sipServices) {
		_smSipServices = sipServices;
	}

	// generate call controller adapter
	private ListAdapter generateCallControllerAdapter() {
		// call controller item adapter data key
		final String CALL_CONTROLLER_ITEM_BACKGROUND = "call_controller_item_background";
		final String CALL_CONTROLLER_ITEM_ICON = "call_controller_item_icon";
		final String CALL_CONTROLLER_ITEM_LABEL = "call_controller_item_label";

		// define call controller gridView content
		final int[][] _callControllerGridViewContentArray = new int[][] {
				{ R.drawable.callcontroller_contactitem_bg,
						R.drawable.img_callcontroller_contactitem_normal,
						R.string.callController_contactItem_text },
				{ R.drawable.callcontroller_keyboarditem_bg,
						R.drawable.img_callcontroller_keyboarditem_normal,
						R.string.callController_keyboardItem_text },
				{ R.drawable.callcontroller_muteitem_bg,
						R.drawable.img_callcontroller_muteitem_normal,
						R.string.callController_muteItem_text },
				{ R.drawable.callcontroller_handfreeitem_bg,
						R.drawable.img_callcontroller_handfreeitem_normal,
						R.string.callController_handfreeItem_text } };

		// set call controller data list
		List<Map<String, ?>> _callControllerDataList = new ArrayList<Map<String, ?>>();

		for (int i = 0; i < _callControllerGridViewContentArray.length; i++) {
			// generate data
			Map<String, Object> _dataMap = new HashMap<String, Object>();

			// put value
			_dataMap.put(CALL_CONTROLLER_ITEM_BACKGROUND,
					_callControllerGridViewContentArray[i][0]);
			_dataMap.put(CALL_CONTROLLER_ITEM_ICON,
					_callControllerGridViewContentArray[i][1]);
			_dataMap.put(CALL_CONTROLLER_ITEM_LABEL,
					_callControllerGridViewContentArray[i][2]);

			// add data to list
			_callControllerDataList.add(_dataMap);
		}

		return new OutgoingCallControllerAdapter(
				this,
				_callControllerDataList,
				R.layout.call_controller_item,
				new String[] { CALL_CONTROLLER_ITEM_BACKGROUND,
						CALL_CONTROLLER_ITEM_ICON, CALL_CONTROLLER_ITEM_LABEL },
				new int[] { R.id.callController_item_relativeLayout,
						R.id.callController_item_iconImgView,
						R.id.callController_item_labelTextView });
	}

	// inner class
	// call controller gridView on item click listener
	class CallControllerGridViewOnItemClickListener implements
			OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d(LOG_TAG,
					"call controller gridView on item click listener, parent = "
							+ parent + ", view = " + view + ", position = "
							+ position + " and id = " + id);

			//
			// ??
		}

	}

	// hangup outgoing call button on click listener
	class HangupOutgoingCallBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// hangup current sip voice call
			if (!SipUtils.hangupSipVoiceCall(88L)) {
				// force finish outgoing call activity
				finish();
			} else {
				// update call state textView text
				((TextView) findViewById(R.id.callState_textView))
						.setText(R.string.end_outgoing_call);

				// sip voice call terminated
				onCallTerminated();
			}
		}

	}

	// hide keyboard button on click listener
	class HideKeyboardBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Log.d(LOG_TAG, "hide keyboard and view = " + v);

			//
			// ??
		}

	}

}
