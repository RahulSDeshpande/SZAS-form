/**
 * 
 */
package com.szas.android.SZASApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.client.ClientProtocolException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.szas.data.FilledQuestionnaireTuple;
import com.szas.data.QuestionnaireTuple;
import com.szas.sync.SyncedElementsHolder;
import com.szas.sync.ToSyncElementsHolder;
import com.szas.sync.local.LocalDAO;
import com.szas.sync.local.LocalSyncHelperImpl;
import com.szas.sync.local.SyncLocalService;
import com.szas.sync.local.SyncLocalServiceResult;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

/**
 * @author pszafer@gmail.com
 * 
 * 
 *         LEGEND: XXX - adnotation FIXME - something wrong TODO - not
 *         implemented yet
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	/**
	 * Log tag
	 */
	private static final String LOGTAG = "SZAS_SYNC_ADAPTER";

	private final AccountManager accountManager;

	boolean isChanged = false;

	private final static class AndroidSyncLocalService implements
			SyncLocalService {

		/**
		 * @param googleAuthentication
		 * 
		 */
		public AndroidSyncLocalService(GoogleAuthentication googleAuthentication) {
			this.googleAuthentication = googleAuthentication;
		}

		/**
		 * GAE URL
		 */
		String gaeUrl = "http://szas-form.appspot.com/";

		/**
		 * GAE sync URL
		 */
		// String gaeSyncUrl = gaeUrl + "sync?oauth_token=";
		GoogleAuthentication googleAuthentication;

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.szas.sync.local.SyncLocalService#sync(java.util.ArrayList,
		 * com.szas.sync.local.SyncLocalServiceResult)
		 */
		@Override
		public void sync(ArrayList<ToSyncElementsHolder> toSyncElementsHolders,
				SyncLocalServiceResult callback) {

			try {
				ArrayList<SyncedElementsHolder> elements = fetchFromNetwork(
						toSyncElementsHolders, googleAuthentication);
				Log.v("Sync", "Callback.onSuccess");
				callback.onSuccess(elements);
			} catch (ClientProtocolException e1) {
				callback.onFailure(e1);
				e1.printStackTrace();
			} catch (IOException e1) {
				callback.onFailure(e1);
				e1.printStackTrace();
			}

		}

		/**
		 * Method to download syncing elements from network
		 * 
		 * @param googleAuthentication
		 *            - googleAuth class
		 * @return
		 * @throws ClientProtocolException
		 * @throws IOException
		 */
		private ArrayList<SyncedElementsHolder> fetchFromNetwork(
				ArrayList<ToSyncElementsHolder> elementsToSync,
				GoogleAuthentication googleAuthentication)
				throws ClientProtocolException, IOException {
			URL url = new URL(gaeUrl + "sync");
			URLConnection conn = url.openConnection();
			conn.addRequestProperty("Content-Type",
					"application/json; charset=UTF-8");
			conn.addRequestProperty("Content-Encoding", "gzip");
			conn.addRequestProperty("Cookie", googleAuthentication
					.getAuthCookie().getName()
					+ "="
					+ googleAuthentication.getAuthCookie().getValue());
			conn.addRequestProperty("User-Agent",
					"Mozilla/5.0 (X11; Linux x86_64; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
			conn.addRequestProperty("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			conn.addRequestProperty("Accept-Language",
					"pl,en-us;q=0.7,en;q=0.3");
			conn.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			conn.addRequestProperty("Accept-Charset", "utf-8;q=0.7,*;q=0.7");
			conn.addRequestProperty("Cache-Control", "no-cache");
			conn.addRequestProperty("Pragma", "no-cache");
			// this do post method
			conn.setDoOutput(true);
			OutputStream outputStream = conn.getOutputStream();
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
					outputStream);
			OutputStreamWriter out = new OutputStreamWriter(gzipOutputStream);
			new JSONSerializer().prettyPrint(true).include("*")
					.serialize(elementsToSync, out);
			out.close();
			gzipOutputStream.close();
			InputStream inputStream = null;
			inputStream = conn.getInputStream();
			String encoding = conn.getContentEncoding();
			String charset = conn.getContentType();
			int startIndex = charset.indexOf("charset=") + 8;
			String word = charset.substring(startIndex).split(" |\"\"")[0];
			if (word == null) {
				word = "utf-8";
			}
			InputStreamReader inputStreamReader;
			if (inputStream == null)
				return null;
			if (encoding.toLowerCase().equals("gzip")) {
				GZIPInputStream gzipInputStream = new GZIPInputStream(
						inputStream);
				inputStreamReader = new InputStreamReader(gzipInputStream, word);
			} else {
				inputStreamReader = new InputStreamReader(inputStream, word);
			}
			ArrayList<SyncedElementsHolder> result = new JSONDeserializer<ArrayList<SyncedElementsHolder>>()
					.deserialize(inputStreamReader);
			return result;
		}

	}

	private LocalDAO<QuestionnaireTuple> questionnaireDAO;

	private LocalDAO<FilledQuestionnaireTuple> filledQuestionnaireDAO;

	private AndroidSyncLocalService syncLocalService;

	private final Context context;

	/**
	 * Constructor to load needed parameters
	 * 
	 * @param context
	 *            context
	 * @param autoInitialize
	 *            needed by syncAdapter
	 */
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
		this.accountManager = AccountManager.get(context);
		questionnaireDAO = new SQLLocalDAO<QuestionnaireTuple>(context,
				"com.szas.data.QuestionnaireTuple");
		filledQuestionnaireDAO = new SQLLocalDAO<FilledQuestionnaireTuple>(
				context, "com.szas.data.FilledQuestionnaireTuple");
		if (questionnaireDAO.getAll().isEmpty())
			questionnaireDAO.setLastTimestamp(-1);
		if (filledQuestionnaireDAO.getAll().isEmpty())
			filledQuestionnaireDAO.setLastTimestamp(-1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.AbstractThreadedSyncAdapter#onPerformSync(android.accounts
	 * .Account, android.os.Bundle, java.lang.String,
	 * android.content.ContentProviderClient, android.content.SyncResult)
	 */
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient contentProviderClient, SyncResult syncResult) {

		boolean bool = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL);
		if (bool) {
			Log.v(LOGTAG, "syncAdapter sync started manually");
		} else {
			Log.v(LOGTAG, "syncAdapter sync started automatically");
		}
		GoogleAuthentication googleAuthentication = GoogleAuthentication
				.getGoogleAuthentication(account);
		if (!googleAuthentication.connect(accountManager)) {
			syncResult.stats.numAuthExceptions++;
			return;
		}
		if (googleAuthentication.getAuthCookie() == null) {
			syncResult.stats.numAuthExceptions++;
			return;
		}
		syncLocalService = new AndroidSyncLocalService(googleAuthentication);
		LocalSyncHelperImpl syncHelper = new LocalSyncHelperImpl(
				syncLocalService);
		syncHelper.append("questionnaire", getQuestionnaireDAO());
		syncHelper.append("filled", filledQuestionnaireDAO);
		registerContentObservers();
		syncHelper.sync();
		if (isChanged) {
			sendMessage("info");
			isChanged = false;
		}
		unregisterContentObservers();
	}

	/**
	 * @param questionnaireDAO
	 *            the questionnaireDAO to set
	 */
	public void setQuestionnaireDAO(
			LocalDAO<QuestionnaireTuple> questionnaireDAO) {
		this.questionnaireDAO = questionnaireDAO;
	}

	/**
	 * @return the questionnaireDAO
	 */
	public LocalDAO<QuestionnaireTuple> getQuestionnaireDAO() {
		return questionnaireDAO;
	}

	public void sendMessage(String information) {
		Intent i = new Intent(Constans.broadcastMessage);
		i.putExtra("info", information);
		context.sendBroadcast(i);
	}

	private void registerContentObservers() {
		ContentResolver cr = context.getContentResolver();
		cr.registerContentObserver(
				DBContentProvider.DatabaseContentHelper.contentUriSyncedElements,
				true, contentObserver);
	}

	private void unregisterContentObservers() {
		ContentResolver cr = context.getContentResolver();
		if (contentObserver != null) {
			cr.unregisterContentObserver(contentObserver);
		}
	}

	private ContentObserver contentObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			isChanged = true;
		};
	};

}
