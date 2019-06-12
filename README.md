<!-- TOC -->

- [1. 简介](#1-简介)
- [2. 部署](#2-部署)
  - [2.1. 必要组件](#21-必要组件)
  - [2.2. 海康SDK环境配置（非必须）](#22-海康sdk环境配置（非必须）)
  - [2.3. 搭建RTMP服务器](#23-搭建rtmp服务器)
  - [2.4. 快速开始](#24-快速开始)
- [3. 设计概要](#3-设计概要)
  - [3.1. 架构图](#31-架构图)
  - [3.2. 流程图](#32-流程图)
  - [3.3. 推拉流协议](#33-推拉流协议)
- [4. 要点说明](#4-要点说明)
  - [4.1. 视频的拉取、推流和存储](#41-视频的拉取推流和存储)
    - [4.1.1. FFmpegFrameGrabber](#411-ffmpegframegrabber)
    - [4.1.2. FFmpegFrameRecorder](#412-ffmpegframerecorder)
  - [4.2. Listener处理Event](#42-listener处理event)
    - [4.2.1. 推流PushListener](#421-推流pushlistener)
    - [4.2.2. 存储RecordListener](#422-存储recordlistener)
    - [4.2.3. 其他Listener](#423-其他listener)
  - [4.3. 回收内存](#43-回收内存)
    - [4.3.1. 原因](#431-原因)
    - [4.3.2. unref](#432-unref)

<!-- /TOC -->

# 1. 简介

RTSP转RTMP/HLS的视频拉流推流程序。

# 2. 部署

## 2.1. 必要组件

- Java([安装教程](https://java.com/en/download/help/download_options.xml))
- maven（[安装过程](http://maven.apache.org/install.html)）
- git（[下载地址](https://git-scm.com/downloads)）
- 海康SDK（非必须）（[下载地址](https://www.hikvision.com/cn/download_61.html)）

## 2.2. 海康SDK环境配置（非必须）

1. 获取之前下载获得的SDK开发包，解压
2. SDK所需要的so库文件都在SDK解压后的lib文件夹下
3. 在系统的/usr/lib文件夹中加入Java工程所需要使用的so文件,然后将SDK解压后的lib文件夹下的HCNetSDKCom文件夹下的其他so文件也copy到/usr/lib目录

注：系统默认加载库路径，cenos64位需拷贝/usr/lib64下

## 2.3. 搭建RTMP服务器

目前有两种选择：

1. 使用red5流媒体服务器作为RTMP服务器（[搭建过程](https://blog.csdn.net/u010651369/article/details/80886676)）
2. 采用NGINX的方案，搭建自己的RTMP服务器（[搭建过程](https://obsproject.com/forum/resources/how-to-set-up-your-own-private-rtmp-server-using-nginx.50/)）

## 2.4. 快速开始

1. 克隆项目代码，这里以保存到~文件为例

```shell
cd ~
git clone https://github.com/czxstc200/converter.git
```

2. 进入项目文件夹，使用maven进行项目构建

```shell
cd converter/
mvn clean install
```

3. 进入controller/target文件夹，找到jar包并且使用Java运行jar包。java命令之后需要加上视频录像的存放根目录，例如/home/rec/，注意需要使用绝对路径

```shell
cd controller/target
```

将命令中的${YOUR_ABSOLUTE_PATH}换作你自己的录像存放地址。

```shell
java -DRootDir=${YOUR_ABSOLUTE_PATH} -jar controller-1.0-SNAPSHOT.jar
```

4. 调用接口进行推流

```shell
curl 127.0.0.1:8083/convert?rtsp=rtsp://admin:LITFYL@10.112.239.157:554/h264/ch1/main/av_stream\\&rtmp=rtmp://10.112.217.199/live360p/test\\&usePacket=true
```

将rtsp和rtmp的地址换成自己的rtsp和rtmp地址即可。

5. 使用VLC等支持RTMP的播放器，输入RTMP地址并打开，查看直播效果。

![直播效果](https://github.com/czxstc200/converter/raw/master/assets/%E6%92%AD%E6%94%BE%E6%95%88%E6%9E%9C.png)

# 3. 设计概要

## 3.1. 架构图

![架构图](https://github.com/czxstc200/converter/raw/master/assets/%E6%9E%B6%E6%9E%84%E5%9B%BE%20.jpg)
项目架构如图：

- 首先会通过FFmpegFrameGrabber进行RTSP视频流的拉取，拉取后封装成Event类，并且将Event传给Listeners。
- Listeners是一组监听器，监听Event事件的发生并且会根据Listener的种类不同对Event执行不同的操作，例如写入文件、推流或者物体检测等。
- 物管理平台获取所需数据进行视频的展示和相应功能的执行。
- 通过摄像头厂商提供的SDK，可以获取摄像头的属性数据，将这些数据上传至物管理。同时，物管理也可以通过SDK对摄像头的相关参数进行控制。

## 3.2. 流程图

![流程图](https://github.com/czxstc200/converter/raw/master/assets/%E6%8B%89%E6%B5%81%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

项目的总体流程如图：

- 当发起视频接入的请求时，首先会启动一个对应的Adapter并且进行初始化。
- 根据是否解码字节流，来选择不同的拉流方式。Grab Packet直接拉取字节流，而Grab Image会进行解码获取frame。
- 将通过Grabber拉取到的视频数据封装成Event，并且调用Listener的相关方法。例如存储视频，推流和物体检测等。
- 在Listener完成自己的逻辑后，需要进行unref操作。通过引用计数的方式将堆外内存进行回收。
- 最后，判断用户是否停止推流操作，如果没有停止，重复上述的拉流操作。否则，关闭各个Listener与Adapter。

## 3.3 推拉流协议

![推拉流协议](https://github.com/czxstc200/converter/raw/master/assets/%E6%8E%A8%E6%B5%81%E6%9E%B6%E6%9E%84%E5%9B%BE.jpg)

- 在拉流端，由于摄像头本身会暴露出自己的RTSP地址，因此采用RTSP协议
- 在推流端，采用的是RTMP和HLS。在延迟上，RTMP较之于HLS较低，但是技术较老，且需要Flash的支持，在PC端浏览器中可以使用Flash进行播放。HLS的延迟较高，但是iOS和Android都可以原生支持这一种协议，可以直接在页面上播放，而RTMP协议并不支持直接播放。因此，在推流端采用了RTMP和HLS两种协议。

## 3.4 ONVIF

对于局域网内支持ONVIF协议的网络摄像头，还可以通过接口调用实现设备发现：

```shell
curl 127.0.0.1:8083/discovery
```

该接口会返回找到的ONVIF设备的IP地址。继续调用接口可以获取设备的RTSP地址，并且使用这个RTSP进行推流。

```shell
curl 127.0.0.1:8083/convertWithIp?ip=${YOUR_CAMERA_IP}\\&username=${YOUR_ONVIF_USERNAME}\\&password=${YOUR_ONVIF_PASSWORD}\\&rtmp=${RTMP_PATH}\\&usePacket=true
```

将${YOUR_CAMERA_IP}换成摄像头的IP地址;

将${YOUR_ONVIF_USERNAME}换成摄像头ONVIF的用户名；

将${YOUR_ONVIF_PASSWORD}换成摄像头ONVIF的密码；

将${RTMP_PATH}换成要推流的RTMP地址。

这个接口通过摄像头IP和ONVIF的用户名和密码进行推流操作。

# 4. 要点说明

## 4.1. 视频的拉取、推流和存储

由于采用Java进行项目的编写，选取了JavaCV这个库来实现视频处理。
JavaCV通过JavaCPP Presets对来自计算机视觉领域（OpenCV，FFmpeg等）的研究人员的常用库的的包装来提供相应的功能，使其更易于在Java平台上使用。
JavaCV中主要涉及到的类有：

### 4.1.1. FFmpegFrameGrabber

相关功能：用于拉取RTSP视频流的视频数据
相关方法：

- grabPacket()，直接拉取视频流的AVPacket
- grabImage()，通过grabPacket()拉取视频数据，并且对视频数据进行解码提取出frame

### 4.1.2. FFmpegFrameRecorder

相关功能：用于视频的推流或者存储
相关方法：

- recordPacket()，将获取到的AVPacket进行推流或者存储
- record()，将解码后的frame进行推流或者存储

## 4.2. Listener处理Event

主要通过一个Queue来存储各个Grabber拉取的Event，并且有一个线程池持续从Queue中取Event进行处理

### 4.2.1. 推流PushListener

- 各个Adapter中拉取的数据被封装为Event后都会存储到PushListener的静态Queue中
- PushListener还拥有一个静态的线程池，该线程池执行一个轮询任务，从Queue中取出Event，并根据Event取出相应的FFmpegFrameRecorder实例
- 如果Queue中有数据，通过FFmpegFrameRecorder进行推流
- 如果Queue中没有数据，线程堵塞

### 4.2.2. 存储RecordListener

- 各个Adapter中拉取的数据被封装为Event后都会存储到PushListener的静态Queue中
- RecordListener还拥有一个静态的线程池。考虑到视频的存储中，对实时性的要求较低，因此该线程池只是以一定的周期执行任务
- 执行的任务会不断的从Queue中获取Event，并根据Event取出相应的FFmpegFrameRecorder实例
- 如果Queue中有数据，通过FFmpegFrameRecorder进行视频数据的存储
- 如果Queue没有数据，该线程任务完成并开始堵塞，直到下一次的定时任务开始

### 4.2.3. 其他Listener

- 其他的功能可以通过实现Listener接口来实现

## 4.3. 回收内存

### 4.3.1. 原因

- 由于JavaCV调用的主要是FFmpeg的库来实现RTSP视频流数据的获取，视频数据是存储在堆外内存上的，Java自带的GC并不能对这部分的内存进行回收
- 在拉取视频流frame的过程中，Grabber使用同一块内存进行frame数据的存放。为了让listener能够正常处理frame，在进行listener的调用时，会在另一块内存先clone一个frame。这个frame需要在listener全都执行完后，进行内存回收。
- 在拉取AVPacket的过程中，由于FFmpegFrameRecorder的recordPacket()完成后，会将对应AVPacket的引用次数减一。当达到0后会释放对应的内存。因此，为了让多个Listener能够使用同一块数据，在调用listener逻辑前，需要先为AVPacket创建多个引用。这些额外创建的引用要保证能够被回收。

### 4.3.2. unref

- 目前通过计数法，计算每一个frame或者AVPacket的引用次数
- 当每一个Listener都完成了自身的功能后，相应的frame或者AVPacket的引用数会降低到0
- 通过提供的native方法将对应的内存释放
