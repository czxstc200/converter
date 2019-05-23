package cn.edu.bupt;

import cn.edu.bupt.util.DirUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Description: 启动类
 * @Author: CZX
 * @CreateDate: 2018/12/2 13:52
 * @Version: 1.0
 */
@Slf4j
@SpringBootApplication
public class ConverterApplication {
	public static void main(String[] args) {

		String path = System.getProperty("RootDir");
		if(path!=null){
			checkPath(path);
		}

		System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0");
		System.setProperty("org.bytedeco.javacpp.maxbytes", "0");
		SpringApplication.run(ConverterApplication.class, args);
	}

	public static void checkPath(String path){
		if(!path.endsWith("/")){
			path+="/";
			System.setProperty("RootDir",path);
		}
		if(!path.startsWith("/")){
			log.error("Wrong store path. Please use absolute path.");
			System.exit(1);
		}
		if (!DirUtil.judeDirExists(path)) {
			log.error("Wrong store path. Please check it.");
			System.exit(1);
		}
	}
}
