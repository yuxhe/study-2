LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := deviceinfo

LOCAL_CERTIFICATE := platform
include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))

