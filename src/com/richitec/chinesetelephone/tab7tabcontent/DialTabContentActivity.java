package com.richitec.chinesetelephone.tab7tabcontent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnUriUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.richitec.chinesetelephone.R;
import com.richitec.chinesetelephone.bean.TelUserBean;
import com.richitec.chinesetelephone.call.OutgoingCallActivity;
import com.richitec.commontoolkit.addressbook.AddressBookManager;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.DpPixUtils;

public class DialTabContentActivity extends Activity {

	private static final String LOG_TAG = "DialTabContentActivity";

	// dial phone button gridView keys
	public static final String DIAL_PHONE_BUTTON_CODE = "dial_phone_button_code";
	public static final String DIAL_PHONE_BUTTON_IMAGE = "dial_phone_button_image";
	public static final String DIAL_PHONE_BUTTON_ONCLICKLISTENER = "dial_phone_button_onClickListener";
	public static final String DIAL_PHONE_BUTTON_ONLONGCLICKLISTENER = "dial_phone_button_onLongClickListener";

	// define dial phone button dtmf sound
	private static final int[] DIALPHONEBUTTON_DTMFARRAY = { R.raw.dtmf_1,
			R.raw.dtmf_2, R.raw.dtmf_3, R.raw.dtmf_4, R.raw.dtmf_5,
			R.raw.dtmf_6, R.raw.dtmf_7, R.raw.dtmf_8, R.raw.dtmf_9,
			R.raw.dtmf_star, R.raw.dtmf_0, R.raw.dtmf_pound };

	// sound pool
	private static final SoundPool SOUND_POOL = new SoundPool(1,
			AudioManager.STREAM_MUSIC, 0);

	// dial phone textView
	private TextView _mDialPhoneTextView;

	// dial phone button dtmf sound pool map
	private static SparseArray<Integer> _mDialPhoneBtnDTMFSoundPoolMap;

	// audio manager
	private AudioManager _mAudioManager;

	// init dial phone button dtmf sound pool map
	public static void initDialPhoneBtnDTMFSoundPoolMap(Context context) {
		// define dial phone button dtmf sound pool map
		_mDialPhoneBtnDTMFSoundPoolMap = new SparseArray<Integer>();

		// add sound
		for (int i = 0; i < DIALPHONEBUTTON_DTMFARRAY.length; i++) {
			_mDialPhoneBtnDTMFSoundPoolMap.put(i, SOUND_POOL.load(context,
					DIALPHONEBUTTON_DTMFARRAY[i], i + 1));
		}
	}

	// doubango ngnEngine instance
	private final NgnEngine NGN_ENGINE = NgnEngine.getInstance();

	// sip server host
	private final String SIP_SERVER_HOST = "112.132.217.13";//"210.56.60.150";

	// doubango ngn registration state broadcast receiver
	private BroadcastReceiver _mRegistrationStateBroadcastReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("DiaTab", "onCreate");
		// set content view
		setContentView(R.layout.dial_tab_content_activity_layout);

		// init dial phone textView
		// set dial phone textView
		_mDialPhoneTextView = (TextView) findViewById(R.id.dial_phone_textView);

		// set its default text
		_mDialPhoneTextView.setText("");

		// set its text watcher
		_mDialPhoneTextView
				.addTextChangedListener(new DialPhoneTextViewTextWatcher());

		// set dial phone button grid view adapter
		((GridView) findViewById(R.id.dial_phoneBtn_gridView))
				.setAdapter(generateDialPhoneButtonAdapter());

		// init audio manager
		_mAudioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);

		// init dial function button and set click and long click listener
		// get add new contact dial function button
		ImageButton _addNewContactFunBtn = (ImageButton) findViewById(R.id.dial_newContact_functionBtn);

		// set its image resource
		_addNewContactFunBtn
				.setImageResource(R.drawable.img_dial_newcontact_btn);

		// set its click listener
		_addNewContactFunBtn
				.setOnClickListener(new AddNewContactDialFunBtnOnClickListener());

		// set dial call button click listener
		((ImageButton) findViewById(R.id.dial_call_functionBtn))
				.setOnClickListener(new CallDialFunBtnOnClickListener());

		// get clear dial phone function button
		ImageButton _clearDialPhoneFunBtn = (ImageButton) findViewById(R.id.dial_clearDialPhone_functionBtn);

		// set its image resource
		_clearDialPhoneFunBtn
				.setImageResource(R.drawable.img_dial_cleardialphone_btn);

		// set its click and long click listener
		_clearDialPhoneFunBtn
				.setOnClickListener(new ClearDialPhoneDialFunBtnOnClickListener());
		_clearDialPhoneFunBtn
				.setOnLongClickListener(new ClearDialPhoneDialFunBtnOnLongClickListener());

		// init doubango ngn registration state broadcast receiver
		_mRegistrationStateBroadcastReceiver = new RegistrationStateBroadcastReceiver();

		// add doubango ngn registration state changed listener
		IntentFilter _intentFilter = new IntentFilter();
		_intentFilter
				.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);

		registerReceiver(_mRegistrationStateBroadcastReceiver, _intentFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater()
				.inflate(R.menu.dial_tab_content_activity_layout, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		// stop the engine
		if (NGN_ENGINE.isStarted()) {
			NGN_ENGINE.stop();
		}

		// release doubango ngn registration state broadcast receiver
		if (_mRegistrationStateBroadcastReceiver != null) {
			unregisterReceiver(_mRegistrationStateBroadcastReceiver);

			_mRegistrationStateBroadcastReceiver = null;
		}
		Log.e("DiaTab", "destroy");
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.e("DiaTab", "onResume");
		// starts the engine
		if (!NGN_ENGINE.isStarted()) {
			if (NGN_ENGINE.start()) {
				Log.d(LOG_TAG, "Engine started :)");
			} else {
				Log.e(LOG_TAG, "Failed to start the engine :(");
			}
		}

		// get doubango ngn sip service
		INgnSipService _sipService = NGN_ENGINE.getSipService();

		// register sip account
		if (NGN_ENGINE.isStarted() && !_sipService.isRegistered()) {
			// register
			sipAccountRegister();
		}
	}

	// generate dial phone button adapter
	private ListAdapter generateDialPhoneButtonAdapter() {
		// dial phone button adapter data key
		final String DIAL_PHONE_BUTTON = "dial_phone_button";

		// define dial phone button gridView image resource content
		final int[] _dialPhoneButtonGridViewImgResourceContentArray = {
				R.drawable.img_dial_1_btn, R.drawable.img_dial_2_btn,
				R.drawable.img_dial_3_btn, R.drawable.img_dial_4_btn,
				R.drawable.img_dial_5_btn, R.drawable.img_dial_6_btn,
				R.drawable.img_dial_7_btn, R.drawable.img_dial_8_btn,
				R.drawable.img_dial_9_btn, R.drawable.img_dial_star_btn,
				R.drawable.img_dial_0_btn, R.drawable.img_dial_pound_btn };

		// set dial phone button data list
		List<Map<String, ?>> _dialPhoneButtonDataList = new ArrayList<Map<String, ?>>();

		for (int i = 0; i < _dialPhoneButtonGridViewImgResourceContentArray.length; i++) {
			// generate data
			Map<String, Object> _dataMap = new HashMap<String, Object>();

			// value map
			Map<String, Object> _valueMap = new HashMap<String, Object>();
			_valueMap.put(DIAL_PHONE_BUTTON_CODE, i);
			_valueMap.put(DIAL_PHONE_BUTTON_IMAGE,
					_dialPhoneButtonGridViewImgResourceContentArray[i]);
			_valueMap.put(DIAL_PHONE_BUTTON_ONCLICKLISTENER,
					new DialPhoneBtnOnClickListener());
			_valueMap.put(DIAL_PHONE_BUTTON_ONLONGCLICKLISTENER,
					new DialPhoneBtnOnLongClickListener());

			// put value
			_dataMap.put(DIAL_PHONE_BUTTON, _valueMap);

			// add data to list
			_dialPhoneButtonDataList.add(_dataMap);
		}

		return new DialPhoneButtonAdapter(this, _dialPhoneButtonDataList,
				R.layout.dial_phone_btn_layout,
				new String[] { DIAL_PHONE_BUTTON },
				new int[] { R.id.dialBtn_imageView });
	}

	// play dial phone button dtmf sound
	private void playDialPhoneBtnDTMFSound(int dialPhoneBtnIndex) {
		// get volume
		float _volume = _mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);

		// play dial phone button dtmf sound with index
		SOUND_POOL.play(_mDialPhoneBtnDTMFSoundPoolMap.get(dialPhoneBtnIndex),
				_volume, _volume, 0, 0, 1f);
	}

	// sip account register
	private void sipAccountRegister() {
		// get doubango ngn sip service
		INgnConfigurationService _configurationService = NGN_ENGINE
				.getConfigurationService();
		INgnSipService _sipService = NGN_ENGINE.getSipService();
		
		TelUserBean telUser = (TelUserBean) UserManager.getInstance().getUser();
		
		Log.d("DiaTabContent", telUser.getVosphone()+":"+telUser.getVosphone_pwd());

		// credentials
		final String SIP_USERNAME = telUser.getVosphone();
		final String SIP_PASSWORD = telUser.getVosphone_pwd();
		final String SIP_DOMAIN = "richitec.com";
		final String SIP_REALM = "richitec.com";
		final int SIP_SERVER_PORT = 5060;

		// Set network
		_configurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G,
				true);
		_configurationService.putBoolean(
				NgnConfigurationEntry.NETWORK_USE_WIFI, true);

		// Set credentials
		_configurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI,
				SIP_USERNAME);
		_configurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU,
				String.format("sip:%s@%s", SIP_USERNAME, SIP_DOMAIN));
		_configurationService.putString(
				NgnConfigurationEntry.IDENTITY_PASSWORD, SIP_PASSWORD);
		_configurationService.putString(
				NgnConfigurationEntry.NETWORK_PCSCF_HOST, SIP_SERVER_HOST);
		_configurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT,
				SIP_SERVER_PORT);
		_configurationService.putString(NgnConfigurationEntry.NETWORK_REALM,
				SIP_REALM);

		// VERY IMPORTANT: commit changes
		_configurationService.commit();

		// register (log in)
		_sipService.register(this);
	}

	// make voice call
	private boolean makeVoiceCall(String phoneNumber) {
		// call phone prefix
		final String CallPhonePrefix = "86";

		boolean _ret = false;

		// generate sip phone number
		final String _sipPhoneUri = NgnUriUtils.makeValidSipUri(String.format(
				"sip:%s@%s", CallPhonePrefix + phoneNumber, SIP_SERVER_HOST));

		// check sip phone uri
		if (_sipPhoneUri == null) {
			Log.e(LOG_TAG, "Failed to normalize sip uri '" + phoneNumber + "'");
		} else {
			// define audio session
			NgnAVSession _audioSession = NgnAVSession.createOutgoingSession(
					NGN_ENGINE.getSipService().getSipStack(),
					NgnMediaType.Audio);

			// define the outgoing call intent
			Intent _outgoingCallIntent = new Intent(this,
					OutgoingCallActivity.class);

			// set outgoing call phone, ownership and audio session id
			_outgoingCallIntent.putExtra(
					OutgoingCallActivity.OUTGOING_CALL_PHONE, phoneNumber);

			// get dial phone ownership textView
			TextView _dialPhoneOwnershipTextView = (TextView) findViewById(R.id.dial_phone_ownership_textView);
			if (View.VISIBLE == _dialPhoneOwnershipTextView.getVisibility()) {
				_outgoingCallIntent.putExtra(
						OutgoingCallActivity.OUTGOING_CALL_OWNERSHIP,
						_dialPhoneOwnershipTextView.getText().toString());
			}

			_outgoingCallIntent.putExtra(
					OutgoingCallActivity.OUTGOING_CALL_SIPSESSIONID,
					_audioSession.getId());

			// start outgoing call activity and make an new call
			startActivity(_outgoingCallIntent);

			_ret = _audioSession.makeCall(_sipPhoneUri);
		}

		return _ret;
	}

	// inner class
	// dial phone textView text watcher
	class DialPhoneTextViewTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			// measure text
			// define textView text bounds and font size
			Rect _textBounds = new Rect();
			Integer _textFontSize;

			// set its default font size
			_mDialPhoneTextView.setTextSize(DpPixUtils.pix2dp(getResources()
					.getDimension(R.dimen.dialPhone_textView_textMaxFontSize)));

			do {
				// get text bounds
				_mDialPhoneTextView.getPaint().getTextBounds(s.toString(), 0,
						s.toString().length(), _textBounds);

				// get text font size
				_textFontSize = DpPixUtils.pix2dp(_mDialPhoneTextView
						.getTextSize());

				// check bounds
				if (_textBounds.right + /* padding left and right */2 * 16 > _mDialPhoneTextView
						.getRight()) {
					// reset dial phone textView text font size
					_mDialPhoneTextView.setTextSize(_textFontSize - 1);
				} else {
					break;
				}
			} while (_textFontSize > DpPixUtils.pix2dp(getResources()
					.getDimension(R.dimen.dialPhone_textView_textMinFontSize)));

			// get the dial phone ownership
			// get dial phone ownership textView
			TextView _dialPhoneOwnershipTextView = (TextView) findViewById(R.id.dial_phone_ownership_textView);

			// get address book manager reference
			AddressBookManager _addressBookManager = AddressBookManager
					.getInstance();

			// check dial phone has ownership
			Long _dialPhoneOwnershipId = _addressBookManager
					.isContactWithPhoneInAddressBook(s.toString());
			if (null != _dialPhoneOwnershipId) {
				// set dial phone ownership textView text and show it
				_dialPhoneOwnershipTextView.setText(_addressBookManager
						.getContactByAggregatedId(_dialPhoneOwnershipId)
						.getDisplayName());

				_dialPhoneOwnershipTextView.setVisibility(View.VISIBLE);
			} else {
				// hide dial phone ownership textView
				_dialPhoneOwnershipTextView.setVisibility(View.GONE);
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

	}

	// dial phone button on click listener
	class DialPhoneBtnOnClickListener implements OnClickListener {

		// define dial phone button value data
		private final String[] _dialPhoneButtonValueData = new String[] { "1",
				"2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#" };

		@Override
		public void onClick(View v) {
			// define dial phone string builder
			StringBuilder _dialPhoneStringBuilder = new StringBuilder(
					_mDialPhoneTextView.getText());

			// dial phone
			_dialPhoneStringBuilder
					.append(_dialPhoneButtonValueData[(Integer) v.getTag()]);

			// reset dial phone textView text
			_mDialPhoneTextView.setText(_dialPhoneStringBuilder);

			// play dial phone button dtmf sound
			playDialPhoneBtnDTMFSound((Integer) v.getTag());
		}

	}

	// dial phone button on long click listener
	class DialPhoneBtnOnLongClickListener implements OnLongClickListener {

		@Override
		public boolean onLongClick(View v) {
			boolean _ret = false;

			// check +
			if (10 == (Integer) v.getTag()) {
				// define dial phone string builder
				StringBuilder _dialPhoneStringBuilder = new StringBuilder(
						_mDialPhoneTextView.getText());

				// +
				_dialPhoneStringBuilder.append('+');

				// reset dial phone textView text
				_mDialPhoneTextView.setText(_dialPhoneStringBuilder);

				// play dial phone button dtmf sound
				playDialPhoneBtnDTMFSound((Integer) v.getTag());

				_ret = true;
			}

			return _ret;
		}

	}

	// add new contact dial function button on click listener
	class AddNewContactDialFunBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// get dial phone text
			String _dialPhoneString = _mDialPhoneTextView.getText().toString();

			// check dial phone string
			if (null != _dialPhoneString
					&& !"".equalsIgnoreCase(_dialPhoneString)) {
				Log.d(LOG_TAG, "add new contact, new contact phone = "
						+ _dialPhoneString);

				Toast.makeText(DialTabContentActivity.this,
						"The new contact phone = " + _dialPhoneString,
						Toast.LENGTH_SHORT).show();
			}
		}

	}

	// call dial function button on click listener
	class CallDialFunBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// get dial phone text
			String _dialPhoneString = _mDialPhoneTextView.getText().toString();

			// check dial phone string
			if (null != _dialPhoneString
					&& !"".equalsIgnoreCase(_dialPhoneString)) {
				// re-register
				sipAccountRegister();

				// make voice call
				makeVoiceCall(_dialPhoneString);
			} else {
				Log.e(LOG_TAG, "The dial phone number is null");
			}
		}

	}

	// clear dial phone dial function button on click listener
	class ClearDialPhoneDialFunBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// get dial phone text
			String _dialPhoneString = _mDialPhoneTextView.getText().toString();

			// check dial phone string
			if (null != _dialPhoneString && _dialPhoneString.length() > 0) {
				// reset dial phone textView text
				_mDialPhoneTextView.setText(_dialPhoneString.substring(0,
						_dialPhoneString.length() - 1));
			}
		}

	}

	// clear dial phone dial function button on long click listener
	class ClearDialPhoneDialFunBtnOnLongClickListener implements
			OnLongClickListener {

		@Override
		public boolean onLongClick(View view) {
			// get dial phone text
			String _dialPhoneString = _mDialPhoneTextView.getText().toString();

			// check dial phone string
			if (null != _dialPhoneString && _dialPhoneString.length() > 0) {
				// clear dial phone textView text
				_mDialPhoneTextView.setText("");
			}

			return true;
		}

	}

	// doubango ngn registration state broadcast receiver
	class RegistrationStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the action
			String _action = intent.getAction();

			// check the action for ngn registration Event
			if (NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT
					.equals(_action)) {
				// get ngn registration event arguments
				NgnRegistrationEventArgs _ngnRegistrationEventArgs = intent
						.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);

				// check the arguments
				if (null == _ngnRegistrationEventArgs) {
					Log.e(LOG_TAG,
							"Doubango ngn registration event arguments is null");
				} else {
					// check registration event type
					switch (_ngnRegistrationEventArgs.getEventType()) {
					case REGISTRATION_NOK:
						Log.d(LOG_TAG, "Failed to register :(");
						break;
					case UNREGISTRATION_OK:
						Log.d(LOG_TAG, "You are now unregistered :)");
						break;
					case REGISTRATION_OK:
						Log.d(LOG_TAG, "You are now registered :)");
						break;
					case REGISTRATION_INPROGRESS:
						Log.d(LOG_TAG, "Trying to register...");
						break;
					case UNREGISTRATION_INPROGRESS:
						Log.d(LOG_TAG, "Trying to unregister...");
						break;
					case UNREGISTRATION_NOK:
						Log.d(LOG_TAG, "Failed to unregister :(");
						break;
					}
				}
			}
		}

	}

}