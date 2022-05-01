//
// Created by Dainius Vaiciulis on 6/11/2020.
//

// Save as "HelloJNI.cpp"
#include <jni.h>       // JNI header provided by JDK
#include <iostream>
#include <sstream>
#include <iomanip>


#define POLY (0x1021)
#define INIT (0xa55a)
#define FXOR (0x0000)    /* final xor == xor out */

using namespace std;

/**
 * Converts JString to std::string
 * since it's not a regular string passed from the APP
 *
 * @param env
 * @param jStr
 * @return
 */
std::string jStringToString(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

/**
 * Converts String to Hex String
 *
 * @param inStr
 * @param outStr
 * @return
 */
bool stringToHex(const std::string& inStr, unsigned char* outStr)
{
    size_t len = inStr.length();
    for (size_t i = 0; i < len; i += 2) {
        sscanf(inStr.c_str() + i, "%2hhx", outStr);
        ++outStr;
    }
    return true;
}

/**
 * Converts Integer To Hex String
 *
 * @param n
 * @return
 */
std::string intToHexString(uint16_t n) {
    std::stringstream ss;
    ss << std::hex << std::setw(4) << std::setfill('0') << n;
    std::string res(ss.str());

    return res;
}

/**
 * Calculates CRC value
 *
 * @param bfr
 * @param size
 * @return
 */
uint16_t crc16(uint8_t* bfr, size_t size)
{
    uint16_t crc = INIT;
    int i;
    while (size--) {
        crc ^= ((uint16_t)(*bfr++)) << 8;
        for (i = 0; i < 8; i++)
            /* assumes two's complement */
            crc = (crc << 1) ^ ((0 - (crc >> 15)) & POLY);
    }

    return(crc ^ FXOR);
}

/**
 * Master function to calculate CRC
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_feyiuremote_libs_Feiyu_FeyiuCrc_calc(JNIEnv *env, jclass clazz, jstring hex_jstring) {
    uint8_t * hex_int;
    std::string hex_string = jStringToString(env, hex_jstring);
    size_t hex_size = hex_string.length() / 2;
    hex_int = new uint8_t[hex_size];
    stringToHex(hex_string, hex_int);

    uint16_t hex_int_crc = crc16(hex_int, hex_size);

    // Swapping two bytes
    hex_int_crc = (hex_int_crc >> 8) | (hex_int_crc << 8);

    hex_string += intToHexString(hex_int_crc);

    hex_jstring = (env)->NewStringUTF(hex_string.c_str());

    return hex_jstring;
}