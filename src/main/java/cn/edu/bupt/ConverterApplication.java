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
		if(args.length==0){
			log.info("Store directory is not specified, use the default path [{}].","/Users/czx/Downloads/");
		}else{
			String path = args[0];
			if(!path.endsWith("/")){
				path+="/";
			}
			if(!path.startsWith("/")){
				log.error("Wrong store path. Please use absolute path.");
				System.exit(1);
			}
			if (DirUtil.judeDirExists(path)) {
				log.info("Store directory is [{}].",path);
				System.setProperty("storeDir",path);
			}else{
				log.error("Wrong store path. Please check it.");
				System.exit(1);
			}
		}
		System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0");
		System.setProperty("org.bytedeco.javacpp.maxbytes", "0");
		SpringApplication.run(ConverterApplication.class, args);
	}
}
