package top.maxim.rtc.webrtc

import im.floo.floolib.BMXVideoConfig
import im.floo.floolib.BMXVideoProfile

/**
 * Description : RTC参数设置
 * Created by Mango on 10/23/21.
 */
class RTCParametersStrategy constructor(build : Builder){

    var videoCallEnabled = false
    var videoWidth = 0
    var videoHeight = 0
    var videoFps = 0
    var videoCodec: String? = null
    var videoCodecHwAcceleration = false
    var videoFlexFecEnabled = false

    init {
        videoCallEnabled = build.videoEnabled
        videoWidth = build.videoWidth
        videoHeight = build.videoHeight
        videoFps = build.videoFps
        videoCodec = build.videoCodec
        videoCodecHwAcceleration = build.videoCodecHwAcceleration
        videoFlexFecEnabled = build.videoFlexFecEnabled
    }

    class Builder {
        var videoEnabled = false
        var videoWidth = 0
        var videoHeight = 0
        var videoFps = 0
        var videoCodec: String? = null
        var videoCodecHwAcceleration = false
        var videoFlexFecEnabled = false

        fun videoEnable(videoEnable : Boolean) : Builder {
            this.videoEnabled = videoEnabled
            return this
        }

        fun videoProfile(videoConfig: BMXVideoConfig?) : Builder {
            val config = buildVideoConfig(videoConfig)
            this.videoWidth = config.width
            this.videoHeight = config.height
            this.videoFps = config.frameRate
            return this
        }

        fun videoCodec(videoCodec : String) : Builder {
            this.videoCodec = videoCodec
            return this
        }

        fun videoCodecHwAcceleration(videoCodecHwAcceleration : Boolean) : Builder {
            this.videoCodecHwAcceleration = videoCodecHwAcceleration
            return this
        }

        fun videoFlexFecEnabled(videoFlexFecEnabled : Boolean) : Builder {
            this.videoFlexFecEnabled = videoFlexFecEnabled
            return this
        }

        fun build() : RTCParametersStrategy{
            if(videoWidth * videoHeight * videoFps == 0){
                //设置默认videoConfig
                val config = BMXVideoConfig()
                config.profile = BMXVideoProfile.Profile_480_360
                videoProfile(config)
            }
            return RTCParametersStrategy(this)
        }

        /**
         * 根据videoProfile返回视频宽高
         */
        private fun buildVideoConfig(videoConfig: BMXVideoConfig?) : BMXVideoConfig{
            val width : Int
            val height : Int
            if(videoConfig == null){
                val config = BMXVideoConfig()
                config.width = 320
                config.height = 480
                config.frameRate = 25
                return config
            }
            if (videoConfig.width > 0 && videoConfig.height > 0 && videoConfig.frameRate > 0) {
                //width  height  fps均有值 直接返回
                return videoConfig
            }
            when(videoConfig.profile){
                BMXVideoProfile.Profile_240_180 -> {
                    width = 240
                    height = 180
                }
                BMXVideoProfile.Profile_320_180 -> {
                    width = 320
                    height = 180
                }
                BMXVideoProfile.Profile_320_240 -> {
                    width = 320
                    height = 240
                }
                BMXVideoProfile.Profile_480_360 -> {
                    width = 480
                    height = 360
                }
                BMXVideoProfile.Profile_640_360 -> {
                    width = 640
                    height = 360
                }
                BMXVideoProfile.Profile_640_480 -> {
                    width = 640
                    height = 480
                }
                BMXVideoProfile.Profile_960_720 -> {
                    width = 960
                    height = 720
                }
                BMXVideoProfile.Profile_1280_720 -> {
                    width = 1280
                    height = 720
                }
                BMXVideoProfile.Profile_1920_1080 -> {
                    width = 1920
                    height = 1080
                }
                else ->{
                    width = 480
                    height = 360
                }
            }
            val config = BMXVideoConfig()
            config.width = width
            config.height = height
            config.frameRate = 25
            return config
        }
    }
}