package com.rotd.havenpad.domain.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

actual fun createSecureStorage(): SecureStorage = IosKeychainSecureStorage()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosKeychainSecureStorage : SecureStorage {
    override fun getString(key: String): String? {
        val query = baseQuery(key).apply {
            setObject(true, forKey = bridged(kSecReturnData))
            setObject(kSecMatchLimitOne, forKey = bridged(kSecMatchLimit))
        }
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return@memScoped null
            val data = result.value as? NSData ?: return@memScoped null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
        }
    }

    override fun putString(key: String, value: String?) {
        delete(key)
        if (value == null) return
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = baseQuery(key).apply {
            setObject(data, forKey = bridged(kSecValueData))
        }
        SecItemAdd(query, null)
    }

    override fun clear() {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = bridged(kSecClass))
            setObject(SERVICE, forKey = bridged(kSecAttrService))
        }
        SecItemDelete(query)
    }

    private fun delete(key: String) {
        SecItemDelete(baseQuery(key))
    }

    private fun baseQuery(account: String): NSMutableDictionary =
        NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = bridged(kSecClass))
            setObject(SERVICE, forKey = bridged(kSecAttrService))
            setObject(account, forKey = bridged(kSecAttrAccount))
        }

    @Suppress("UNCHECKED_CAST")
    private fun bridged(key: Any?): NSCopyingProtocol =
        key as NSCopyingProtocol

    companion object {
        private const val SERVICE = "com.rotd.havenpad.secure"
    }
}
