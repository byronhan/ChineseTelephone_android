package com.richitec.chinesetelephone.sip.services;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession.InviteState;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnUriUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.richitec.chinesetelephone.sip.SipRegisterBean;
import com.richitec.chinesetelephone.sip.listeners.SipInviteStateListener;
import com.richitec.chinesetelephone.sip.listeners.SipRegistrationStateListener;
import com.richitec.commontoolkit.activityextension.AppLaunchActivity;

public class DoubangoSipServices extends BaseSipServices implements
		ISipServices {

	private static final String LOG_TAG = "DoubangoSipServices";

	// doubango ngn engine instance
	private final NgnEngine NGN_ENGINE = NgnEngine.getInstance();

	// sip registration state listener
	private SipRegistrationStateListener _mSipRegistrationStateListener;

	// doubango ngn registration state broadcast receiver
	private BroadcastReceiver _mRegistrationStateBroadcastReceiver;

	// doubango ngn audio/video session state broadcast receiver
	private BroadcastReceiver _mAVSessionStateBroadcastReceiver;

	// doubango current sip voice call ngn audio session
	private NgnAVSession _mSipVoiceCallSession;

	public DoubangoSipServices() {
		super();

		// get application context
		Context _appContext = AppLaunchActivity.getAppContext();

		// register
		// init doubango ngn registration state broadcast receiver
		_mRegistrationStateBroadcastReceiver = new RegistrationStateBroadcastReceiver();

		// register sip registration state broadcast receiver
		// new sip register intent filter
		IntentFilter _sipRegisterIntentFilter = new IntentFilter();
		_sipRegisterIntentFilter
				.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);

		_appContext.registerReceiver(_mRegistrationStateBroadcastReceiver,
				_sipRegisterIntentFilter);

		// invite
		// init doubango ngn audio/video session state broadcast receiver
		_mAVSessionStateBroadcastReceiver = new AVSessionStateBroadcastReceiver();

		// register doubango ngn audio/video session state receiver
		// new sip invite intent filter
		IntentFilter _sipInviteIntentFilter = new IntentFilter();
		_sipInviteIntentFilter
				.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);

		_appContext.registerReceiver(_mAVSessionStateBroadcastReceiver,
				_sipInviteIntentFilter);
	}

	@Override
	public void registerSipAccount(SipRegisterBean sipAccount,
			SipRegistrationStateListener sipRegistrationStateListener) {
		// update sip registration state listener
		_mSipRegistrationStateListener = sipRegistrationStateListener;

		// starts ngn engine
		if (!NGN_ENGINE.isStarted()) {
			if (NGN_ENGINE.start()) {
				Log.d(LOG_TAG, "Ngn engine started :)");
			} else {
				Log.e(LOG_TAG, "Failed to start ngn engine :(");
			}
		} else {
			Log.d(LOG_TAG, "Ngn engine had been started");
		}

		// get doubango ngn sip service
		INgnSipService _sipService = NGN_ENGINE.getSipService();

		// register sip account
		if (NGN_ENGINE.isStarted() && !_sipService.isRegistered()) {
			// get doubango ngn config service
			INgnConfigurationService _configurationService = NGN_ENGINE
					.getConfigurationService();

			// set network, use both 3g and wifi
			_configurationService.putBoolean(
					NgnConfigurationEntry.NETWORK_USE_3G, true);
			_configurationService.putBoolean(
					NgnConfigurationEntry.NETWORK_USE_WIFI, true);

			// set credentials
			_configurationService.putString(
					NgnConfigurationEntry.IDENTITY_IMPI,
					sipAccount.getSipUserName());
			_configurationService.putString(
					NgnConfigurationEntry.IDENTITY_IMPU, String.format(
							"sip:%s@%s", sipAccount.getSipUserName(),
							sipAccount.getSipDomain()));
			_configurationService.putString(
					NgnConfigurationEntry.IDENTITY_PASSWORD,
					sipAccount.getSipPwd());
			_configurationService.putString(
					NgnConfigurationEntry.NETWORK_PCSCF_HOST,
					sipAccount.getSipServer());
			_configurationService.putString(
					NgnConfigurationEntry.NETWORK_REALM,
					sipAccount.getSipRealm());
			_configurationService.putInt(
					NgnConfigurationEntry.NETWORK_PCSCF_PORT,
					sipAccount.getSipPort());

			// commit changes
			_configurationService.commit();

			// sip account register
			_sipService.register(AppLaunchActivity.getAppContext());
		}
	}

	@Override
	public void unregisterSipAccount(
			SipRegistrationStateListener sipRegistrationStateListener) {
		// get doubango ngn sip service
		INgnSipService _sipService = NGN_ENGINE.getSipService();

		// unregister sip account
		if (NGN_ENGINE.isStarted() && _sipService.isRegistered()) {
			_sipService.unRegister();
		}
	}

	@Override
	public boolean makeDirectDialSipVoiceCall(String calleeName,
			String calleePhone) {
		// define return result
		boolean _ret = false;

		// get doubango ngn sip service
		INgnSipService _sipService = NGN_ENGINE.getSipService();

		// re-register
		if (!_sipService.isRegistered()) {
			_sipService.register(AppLaunchActivity.getAppContext());
		}

		// generate callee sip phone number uri
		final String _sipPhoneUri = NgnUriUtils.makeValidSipUri(String.format(
				"sip:%s@%s",
				calleePhone,
				NGN_ENGINE.getConfigurationService().getString(
						NgnConfigurationEntry.NETWORK_PCSCF_HOST, "")));

		// check callee sip phone number uri
		if (null == _sipPhoneUri) {
			Log.e(LOG_TAG, "Failed to normalize callee sip phone number uri '"
					+ calleePhone + "'");
		} else {
			// define get current ngn audio session
			_mSipVoiceCallSession = NgnAVSession.createOutgoingSession(
					NGN_ENGINE.getSipService().getSipStack(),
					NgnMediaType.Audio);

			// doubango ngn audio session make call
			_ret = _mSipVoiceCallSession.makeCall(_sipPhoneUri);
		}

		return _ret;
	}

	@Override
	public boolean makeCallbackSipVoiceCall(String calleeName,
			String calleePhone) {
		Log.d(LOG_TAG, "Not implement make callback sip voice call now");

		return true;
	}

	@Override
	public boolean hangupSipVoiceCall() {
		// define return result
		boolean _ret = false;

		// increase doubango ngn audio/video session reference and set context
		_mSipVoiceCallSession.incRef();
		_mSipVoiceCallSession.setContext((Context) getSipInviteStateListener());

		// check doubango ngn audio/video session
		if (null != _mSipVoiceCallSession) {
			_ret = _mSipVoiceCallSession.hangUpCall();
		} else {
			Log.e(LOG_TAG,
					"Doubango ngn audio/video session is null, force finish outgoing call activity");
		}

		// release doubango ngn audio/video session state broadcast receiver
		if (null != _mAVSessionStateBroadcastReceiver) {
			AppLaunchActivity.getAppContext().unregisterReceiver(
					_mAVSessionStateBroadcastReceiver);

			_mAVSessionStateBroadcastReceiver = null;
		}

		// release doubango ngn audio/video session
		if (null != _mSipVoiceCallSession) {
			_mSipVoiceCallSession.setContext(null);

			_mSipVoiceCallSession.decRef();
		}

		return _ret;
	}

	// inner class
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
					case REGISTRATION_OK:
						Log.d(LOG_TAG, "You are now registered :)");

						_mSipRegistrationStateListener.onRegisterSuccess();

						break;
					case REGISTRATION_NOK:
						Log.d(LOG_TAG, "Failed to register :(");

						_mSipRegistrationStateListener.onRegisterFailed();

						break;
					case UNREGISTRATION_OK:
						Log.d(LOG_TAG, "You are now unregistered :)");

						_mSipRegistrationStateListener.onUnRegisterSuccess();

						break;
					case UNREGISTRATION_NOK:
						Log.d(LOG_TAG, "Failed to unregister :(");

						_mSipRegistrationStateListener.onUnRegisterFailed();

						break;
					case REGISTRATION_INPROGRESS:
						Log.d(LOG_TAG, "Trying to register...");

						break;
					case UNREGISTRATION_INPROGRESS:
						Log.d(LOG_TAG, "Trying to unregister...");

						break;
					}
				}
			}
		}

	}

	// doubango ngn audio/video session state broadcast receiver
	class AVSessionStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// check ngn audio/video session
			if (null == _mSipVoiceCallSession) {
				Log.e(LOG_TAG, "Doubango ngn audio/video session is null");
			} else {
				// get the action
				String _action = intent.getAction();

		// generate sip phone number
		final String _sipPhoneUri = NgnUriUtils
				.makeValidSipUri(String.format(
						"sip:%s@%s",
						calleePhoneNumber,
						NGN_ENGINE.getConfigurationService().getString(
								NgnConfigurationEntry.NETWORK_PCSCF_HOST, "")));

		// check sip phone uri
		if (_sipPhoneUri == null) {
			Log.e(LOG_TAG, "Failed to normalize sip uri '" + calleePhoneNumber
					+ "'");
		} else {
			// define audio session
			NgnAVSession _audioSession = NgnAVSession.createOutgoingSession(
					NGN_ENGINE.getSipService().getSipStack(),
					NgnMediaType.Audio);
				// check the action for ngn invite event
				if (NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(_action)) {
					// get ngn invite event arguments
					NgnInviteEventArgs _ngnInviteEventArgs = intent
							.getParcelableExtra(NgnInviteEventArgs.EXTRA_EMBEDDED);
					Short _ngnInviteEventArgsSipCode = intent.getShortExtra(
							NgnInviteEventArgs.EXTRA_SIPCODE, (short) 200);

					// check the arguments
					if (null == _ngnInviteEventArgs) {
						Log.e(LOG_TAG,
								"Doubango ngn invite event arguments is null");
					} else if (_mSipVoiceCallSession.getId() != _ngnInviteEventArgs
							.getSessionId()) {
						Log.e(LOG_TAG,
								"Doubango ngn audio/video session invalid");
					} else {
						// get the ngn invite state
						InviteState _inviteState = _mSipVoiceCallSession
								.getState();

						Log.d(LOG_TAG,
								"AVSessionStateBroadcastReceiver on receive invite state = "
										+ _inviteState);

						// get sip invite listener
						SipInviteStateListener _sipInviteStateListener = getSipInviteStateListener();

						// check ngn invite state
						switch (_inviteState) {
						case REMOTE_RINGING:
							NGN_ENGINE.getSoundService().startRingBackTone();

							_sipInviteStateListener.onCallRemoteRinging();

							break;

						case EARLY_MEDIA:
							_sipInviteStateListener.onCallEarlyMedia();

							_sipInviteStateListener.onCallEarlyMedia();

							break;

						case INCALL:
							NGN_ENGINE.getSoundService().stopRingBackTone();

							_mSipVoiceCallSession.setSpeakerphoneOn(false);

							_sipInviteStateListener.onCallSpeaking();

							break;

						case TERMINATING:
							_sipInviteStateListener.onCallTerminating();

							break;

						case TERMINATED:
							NGN_ENGINE.getSoundService().stopRingBackTone();

							// check invite event argument sip code
							switch (_ngnInviteEventArgsSipCode) {
							case 480:
								_sipInviteStateListener.onCallFailed();

								break;

							case 200:
							default:
								_sipInviteStateListener.onCallTerminated();

								break;
							}

							break;

						default:
							Log.d(LOG_TAG, "Doubango other invite state = "
									+ _inviteState);

							break;
						}
					}
				}
			}
		}

	}

}
