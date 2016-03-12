package com.cube.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;
import com.cube.logic.HttpProcess;
import com.cube.logic.HttpProcessRunnable;
import com.cube.server.CubeBootstrap;
import com.cube.server.CuratorBootstrap;
import com.cube.server.HttpBootstrap;

/** Spring启动类
* @ClassName: Service 
* @Description: TODO
* @author Wenbo Shen 
* @date Mar 12, 2016 4:48:30 PM 
*  
*/
@Component
public class Service implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(Service.class);

	private final static ExecutorService threadPool = Executors.newCachedThreadPool();
	private boolean started = false;
	private Future future;

	@Value("${rompath}")
	private String rompath;
	@Value("${rompathmcu}")
	private String rompathMcu;

	@Autowired
	private CubeBootstrap cubeBoostrap;
	// @Autowired
	public CuratorBootstrap curatorBootstrap;
	@Autowired
	private HttpBootstrap httpBootstrap;
//	@Autowired
//	private DbManager dbManager;

	@Override
	public void start() {
		LOG.info("serverRun start");

		started = true;
		future = threadPool.submit(cubeBoostrap);

		int sleepCount = 5;
		while (!cubeBoostrap.isRun()) {
			if (sleepCount < 0) {
				future.cancel(true);
				return;
			}
			try {
				sleepCount--;
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOG.info("InterruptedException:", e);
				e.printStackTrace();
			}
		}
		// threadPool.submit(curatorBootstrap);

		// 初始化路由
		initRoute();
		System.setProperty("rompath", rompath);
		System.setProperty("rompathMcu", rompathMcu);
		threadPool.submit(httpBootstrap);

//		LOG.info("dbManager:{}", dbManager.toString());
		
		//数据库连接测试
//		Connection connection = null;
//		PreparedStatement st = null;
//		try {
//			connection = dbManager.getConnection();
//			String sql = "select * from led_romupdate_log";
//			st = connection.prepareStatement(sql);
//			ResultSet rst = st.executeQuery();
//			if(rst.next()){
//				LOG.info("id:{}", rst.getString(1));
//				LOG.info("mac:{}", rst.getString(2));
//				LOG.info("type:{}", rst.getString(3));
//				LOG.info("ins_tm:{}", rst.getString(4));
//				LOG.info("upd_tm:{}", rst.getString(5));
//				LOG.info("pre_version:{}", rst.getString(6));
//				LOG.info("now_version:{}", rst.getString(7));
//			}
//
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				if (connection != null) {
//					dbManager.close(connection);
//				}
//				if(st != null){
//					st.close();
//				}
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//		}

	}

	// 初始化路由
	private void initRoute() {
		try {
			String pkg = "com.cube.logic.http";
			URL root = this.getClass().getResource("/route");
			InputStream in = this.getClass().getResourceAsStream("/route");
			LOG.info("root={}", root);
			// FileReader fr = new FileReader(root.getPath());
			InputStreamReader inReader = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(inReader);
			String line = null;
			String[] args = null;
			while ((line = br.readLine()) != null) {
				args = line.split("\\s{1,}");
				if (args.length != 2) {
					continue;
				}

				Class<?> clazz = this.getClass().forName(pkg + "." + args[1]);
				Class<?>[] ifaces = clazz.getInterfaces();
				if (!isInterface(clazz, HttpProcess.class)) {
					LOG.info("{} is not the HttpProcess", clazz.getCanonicalName());
					continue;
				}

				HttpProcess process = (HttpProcess) clazz.newInstance();
				HttpProcessRunnable.ROUTE.put(args[0], process);
				if (args[0].endsWith("/") == false) {
					HttpProcessRunnable.ROUTE.put(args[0] + "/", process);
				} else {
					HttpProcessRunnable.ROUTE.put(args[0].substring(0, args[0].length() - 2), process);
				}
			}
		} catch (FileNotFoundException e) {
			LOG.error("初始化路由文件没找到", e);
		} catch (IOException e) {
			LOG.error("初始化路由出错", e);
		} catch (ClassNotFoundException e) {
			LOG.error("初始化路由出错", e);
		} catch (InstantiationException e) {
			LOG.error("初始化路由出错", e);
		} catch (IllegalAccessException e) {
			LOG.error("初始化路由出错", e);
		}

	}

	@Override
	public void stop() {
		System.out.println("serverRun stop");
		started = false;
		threadPool.shutdown();
		if (future != null) {
			future.cancel(true);
		}
	}

	@Override
	public boolean isRunning() {
		System.out.println("serverRun isRunning");
		return started;
	}

	public boolean isInterface(Class clazz, Class infClazz) {
		if (clazz == Object.class) {
			return false;
		}
		Class[] ifaces = clazz.getInterfaces();
		boolean trueType = false;
		for (Class<?> iface : ifaces) {
			if (iface.equals(infClazz)) {
				trueType = true;
				break;
			}
		}
		if (trueType) {
			return true;
		}

		clazz = clazz.getSuperclass();
		return isInterface(clazz, infClazz);

	}

}
