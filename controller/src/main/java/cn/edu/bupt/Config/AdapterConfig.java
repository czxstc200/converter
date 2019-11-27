package cn.edu.bupt.Config;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.adapter.VideoAdapterManagement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfig {

    @Bean
    public VideoAdapterManagement<RTSPVideoAdapter> videoAdapterManagement() {
        return new VideoAdapterManagement<>();
    }
}
