/*
 ============================================================================
 Name        : TProxyService.java
 Author      : hev <r@hev.cc>
 Copyright   : Copyright (c) 2024 xyz
 Description : TProxy Service
 ============================================================================
 */

package hev.sockstun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.IpPrefix;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.navbarbutton.MainActivity;
import com.example.navbarbutton.SocksService;
import com.topjohnwu.superuser.ipc.RootService;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TProxyService extends VpnService {
	private static native boolean TProxyStartService(String config_path, int fd);
	private static native boolean TProxyStopService();
	private static native boolean TProxyIsRunning();
	private static native long[] TProxyGetStats();

	public static final String ACTION_CONNECT = "hev.sockstun.CONNECT";
	public static final String ACTION_DISCONNECT = "hev.sockstun.DISCONNECT";

	static {
		System.loadLibrary("hev-socks5-tunnel");
	}

	private ParcelFileDescriptor tunFd = null;

	private ServiceConnection conn = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		initNotificationChannel("socks5");
		createNotification("socks5");
		if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
			stopService();
			return START_NOT_STICKY;
		}
		startService();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onRevoke() {
		stopService();
		super.onRevoke();
	}

	public void startService() {
		SocksService.vpnService = this;
		if (tunFd != null)
		  return;

		String crelay = this.getSharedPreferences("data", Context.MODE_PRIVATE).getString("relay", "rigid.top:80");
		String cdb = new File(getFilesDir(), "oh.db").getAbsolutePath() + ";" + getExternalFilesDir(null);
		RootService.bind(new Intent(this, SocksService.class) {{
			putExtra("relay", crelay);
			putExtra("db", cdb);
		}}, conn = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			}
			@Override
			public void onServiceDisconnected(ComponentName componentName) {
			}
		});

		/* VPN */
		String session = "";
		VpnService.Builder builder = new VpnService.Builder();
		builder.setBlocking(false);
		builder.setMtu(1500);
		// IPv4
		{
			builder.addAddress( "198.18.0.1", 32);
			builder.addRoute("0.0.0.0", 0);
			// builder.addRoute("::", 0);
			// try {
			// 	builder.excludeRoute(new IpPrefix(InetAddress.getByName(crelay.split(":")[0]), 32));
			// } catch (Exception ignored) {
			// }
			session += "IPv4";
		}
		// if (prefs.getIpv6()) {
		// 	String addr = prefs.getTunnelIpv6Address();
		// 	int prefix = prefs.getTunnelIpv6Prefix();
		// 	String dns = prefs.getDnsIpv6();
		// 	builder.addAddress(addr, prefix);
		// 	builder.addRoute("::", 0);
		// 	if (!prefs.getRemoteDns() && !dns.isEmpty())
		// 	  builder.addDnsServer(dns);
		// 	if (!session.isEmpty())
		// 	  session += " + ";
		// 	session += "IPv6";
		// }
		String mappedDns = "198.18.0.2";
		builder.addDnsServer(mappedDns);
		session += "/Global";

		if (true) {
			try {
				PackageManager pm = getPackageManager();
				Set<String> installs = pm.getInstalledApplications(PackageManager.GET_META_DATA).stream().map(a -> a.packageName).collect(Collectors.toSet());
				for (String app : loadVpnWhitelist(getExternalFilesDir(null))) {
					if (!app.isEmpty() && installs.contains(app)) {
						builder.addAllowedApplication(app);
						Log.d("navbarbutton", "addAllowedApplication whitelist: " + app);
					}
				}
			} catch (NameNotFoundException e) {
				Log.e("navbarbutton", "addAllowedApplication", e);
			}
		}

		builder.setSession(session);
		tunFd = builder.establish();
		if (tunFd == null) {
			Log.e("navbarbutton", "builder.establish failed");
			stopSelf();
			return;
		}

		/* TProxy */
		File tproxy_file = new File(getCacheDir(), "tproxy.conf");
		try {
			tproxy_file.createNewFile();
			FileOutputStream fos = new FileOutputStream(tproxy_file, false);

			String tproxy_conf = "misc:\n" +
				"  task-stack-size: 81920\n" +
				"tunnel:\n" +
				"  mtu: 1500\n" +
				"  icmp: 'reply'\n";

			tproxy_conf += "socks5:\n" +
				"  port: 9999\n" +
				"  address: '127.0.0.1'\n" +
				"  udp: 'udp'\n";

			tproxy_conf += "  udp-address: '127.0.0.1'\n";
			tproxy_conf += "mapdns:\n" +
					"  address: 198.18.0.2\n" +
					"  port: 53\n" +
					"  network: 240.0.0.0\n" +
					"  netmask: 240.0.0.0\n" +
					"  cache-size: 10000\n";

			tproxy_conf += "misc:\n" +
					"  log-level: warn\n" +
					"  log-file: " + new File(getExternalFilesDir(null), "tproxy.log");

			fos.write(tproxy_conf.getBytes());
			fos.close();
		} catch (IOException e) {
			Log.e("navbarbutton", "write file", e);
			return;
		}
		TProxyStartService(tproxy_file.getAbsolutePath(), tunFd.getFd());
		getSharedPreferences("data", MODE_PRIVATE).edit().putBoolean("vpn", true).apply();

		String channelName = "socks5";
		initNotificationChannel(channelName);
		createNotification(channelName);
	}

	public void stopService() {
		if (tunFd == null)
		  return;

		getSharedPreferences("data", MODE_PRIVATE).edit().putBoolean("vpn", false).apply();

		stopForeground(true);

		/* TProxy */
		TProxyStopService();

		/* VPN */
		try {
			tunFd.close();
		} catch (IOException e) {
		}
		tunFd = null;

		if (conn != null)
			RootService.unbind(conn);
		// System.exit(0);
	}

	private void createNotification(String channelName) {
		Intent i = new Intent(this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
		NotificationCompat.Builder notification = new NotificationCompat.Builder(this, channelName);
		Notification notify = notification
				.setContentTitle("Navbarbutton")
				.setSmallIcon(android.R.drawable.sym_def_app_icon)
				.setContentIntent(pi)
				.build();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			startForeground(1, notify);
		} else {
			startForeground(1, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
		}
	}

	// create NotificationChannel
	private void initNotificationChannel(String channelName) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = "Navbarbutton";
			NotificationChannel channel = new NotificationChannel(channelName, name, NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
		}
	}

	public static List<String> loadVpnWhitelist(File dir) {
		File backup = new File(dir, "vpn-whitelist.txt");
		try (BufferedReader rd = new BufferedReader(new FileReader(backup))) {
			return rd.lines().toList();
		} catch (IOException e) {
			Log.e("navbarbutton", "addAllowedApplication read backup", e);
		}
		return List.of();
	}

	public static void saveVpnWhitelist(File dir, Collection<String> lines) {
		File backup = new File(dir, "vpn-whitelist.txt");
		try (BufferedWriter wr = new BufferedWriter(new FileWriter(backup))) {
			for (String line : lines) {
				line = line.trim();
				if (!line.isEmpty()) {
					wr.write(line);
					wr.write("\n");
				}
			}
			wr.flush();
		} catch (IOException e) {
			Log.e("navbarbutton", "addAllowedApplication write backup", e);
		}
	}

	public static HashSet<String> whitelistApps = new HashSet<>();

	public static void requestWhitelist(String action, String app) {
		if (action.isEmpty() && !whitelistApps.isEmpty())
			return;
		try {
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(15, TimeUnit.SECONDS)
					.readTimeout(15, TimeUnit.SECONDS)
					.build();

			Request request = new Request.Builder()
					.url("https://rigid.top/android-whitelisted-apps?action=" + action + "&app=" + app)
					// .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
					// .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
					.build();

			try (Response response = client.newCall(request).execute()) {
				if (response.isSuccessful() && response.body() != null) {
					HashSet<String> tmp = new HashSet<>();
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(response.body().byteStream(), "UTF-8"))) {
						String line;
						while ((line = reader.readLine()) != null) {
							if (!line.isEmpty())
								tmp.add(line);
						}
					}
					whitelistApps.clear();
					whitelistApps.addAll(tmp);
				}
			}
		} catch (Exception e) {
			Log.e("navbarbutton", "request whitelist", e);
		}
	}
}
