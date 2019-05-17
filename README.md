# 简介
RTSP转RTMP的视频拉流推流程序。

# 部署

## 必要组件
- maven（[安装过程](http://maven.apache.org/install.html)）
- git（[下载地址](https://git-scm.com/downloads)）
- 海康SDK（[下载地址](https://www.hikvision.com/cn/download_61.html)）

## 部署步骤

TODO

# 设计概要
## 架构图
![架构图](https://github.com/czxstc200/converter/raw/master/assets/%E6%9E%B6%E6%9E%84%E5%9B%BE%20.jpg)
项目架构如图：
- 首先会通过FFmpegFrameGrabber进行RTSP视频流的拉取，拉取后封装成Event类，并且将Event传给Listeners。
- Listeners是一组监听器，监听Event事件的发生并且会根据Listener的种类不同对Event执行不同的操作，例如写入文件、推流或者物体检测等。
- 物管理平台获取所需数据进行视频的展示和相应功能的执行。
- 通过摄像头厂商提供的SDK，可以获取摄像头的属性数据，将这些数据上传至物管理。同时，物管理也可以通过SDK对摄像头的相关参数进行控制。

## 流程图
![流程图](https://github.com/czxstc200/converter/raw/master/assets/%E6%8B%89%E6%B5%81%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

项目的总体流程如图：
- 当发起视频接入的请求时，首先会启动一个对应的Adapter并且进行初始化。
- 根据是否解码字节流，来选择不同的拉流方式。Grab Packet直接拉取字节流，而Grab Image会进行解码获取frame。
- 将通过Grabber拉取到的视频数据封装成Event，并且调用Listener的相关方法。例如存储视频，推流和物体检测等。
- 在Listener完成自己的逻辑后，需要进行unref操作。通过引用计数的方式将堆外内存进行回收。
- 最后，判断用户是否停止推流操作，如果没有停止，重复上述的拉流操作。否则，关闭各个Listener与Adapter。
# 要点说明
## 视频的拉取、推流和存储
由于采用Java进行项目的编写，选取了JavaCV这个库来实现视频处理。
JavaCV通过JavaCPP Presets对来自计算机视觉领域（OpenCV，FFmpeg等）的研究人员的常用库的的包装来提供相应的功能，使其更易于在Java平台上使用。
JavaCV中主要涉及到的类有：
### FFmpegFrameGrabber
相关功能：用于拉取RTSP视频流的视频数据
相关方法：
- grabPacket()，直接拉取视频流的AVPacket
- grabImage()，通过grabPacket()拉取视频数据，并且对视频数据进行解码提取出frame

### FFmpegFrameRecorder
相关功能：用于视频的推流或者存储
相关方法：
- recordPacket()，将获取到的AVPacket进行推流或者存储
- record()，将解码后的frame进行推流或者存储

## Listener处理Event
主要通过一个Queue来存储各个Grabber拉取的Event，并且有一个线程池持续从Queue中取Event进行处理
### 推流PushListener
- 各个Adapter中拉取的数据被封装为Event后都会存储到PushListener的静态Queue中
- PushListener还拥有一个静态的线程池，该线程池执行一个轮询任务，从Queue中取出Event，并根据Event取出相应的FFmpegFrameRecorder实例
- 如果Queue中有数据，通过FFmpegFrameRecorder进行推流
- 如果Queue中没有数据，线程堵塞
### 存储RecordListener
- 各个Adapter中拉取的数据被封装为Event后都会存储到PushListener的静态Queue中
- RecordListener还拥有一个静态的线程池。考虑到视频的存储中，对实时性的要求较低，因此该线程池只是以一定的周期执行任务
- 执行的任务会不断的从Queue中获取Event，并根据Event取出相应的FFmpegFrameRecorder实例
- 如果Queue中有数据，通过FFmpegFrameRecorder进行视频数据的存储
- 如果Queue没有数据，该线程任务完成并开始堵塞，直到下一次的定时任务开始
### 其他Listener
- 其他的功能可以通过实现Listener接口来实现

## 回收内存
### 原因
- 由于JavaCV调用的主要是FFmpeg的库来实现RTSP视频流数据的获取，视频数据是存储在堆外内存上的，Java自带的GC并不能对这部分的内存进行回收
- 在拉取视频流frame的过程中，Grabber使用同一块内存进行frame数据的存放。为了让listener能够正常处理frame，在进行listener的调用时，会在另一块内存先clone一个frame。这个frame需要在listener全都执行完后，进行内存回收。
- 在拉取AVPacket的过程中，由于FFmpegFrameRecorder的recordPacket()完成后，会将对应AVPacket的引用次数减一。当达到0后会释放对应的内存。因此，为了让多个Listener能够使用同一块数据，在调用listener逻辑前，需要先为AVPacket创建多个引用。这些额外创建的引用要保证能够被回收。

### unref
- 目前通过计数法，计算每一个frame或者AVPacket的引用次数
- 当每一个Listener都完成了自身的功能后，相应的frame或者AVPacket的引用数会降低到0
- 通过提供的native方法将对应的内存释放

