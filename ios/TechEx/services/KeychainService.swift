/**
* Inspired by: http://matthewpalmer.net/blog/2014/06/21/example-ios-keychain-swift-save-query/
*/
import UIKit
import Security


// Arguments for the keychain queries
let kSecClassValue = NSString(format: kSecClass)
let kSecAttrAccountValue = NSString(format: kSecAttrAccount)
let kSecValueDataValue = NSString(format: kSecValueData)
let kSecClassGenericPasswordValue = NSString(format: kSecClassGenericPassword)
let kSecAttrServiceValue = NSString(format: kSecAttrService)
let kSecMatchLimitValue = NSString(format: kSecMatchLimit)
let kSecReturnDataValue = NSString(format: kSecReturnData)
let kSecMatchLimitOneValue = NSString(format: kSecMatchLimitOne)

class KeychainService: NSObject {
    
    enum KeychainKey {
        case Username;
        case PlayerId;
    }
    
    /**
    * Exposed methods to perform queries.
    */
    internal class func save(key: KeychainKey, value: NSString) {
//        NSUserDefaults.standardUserDefaults().setValue(value, forKey: keyAsString(key))
        let serviceIdentifier = NSBundle.mainBundle().bundleIdentifier!
        self.save(serviceIdentifier, key: key, value: value)
    }
    
    internal class func load(key: KeychainKey) -> NSString? {
//        return NSUserDefaults.standardUserDefaults().objectForKey(keyAsString(key)) as? String
        let serviceIdentifier = NSBundle.mainBundle().bundleIdentifier!
        var token = self.load(serviceIdentifier, key: key)
        return token
    }
    
    internal class func deleteAll() {
        let serviceIdentifier = NSBundle.mainBundle().bundleIdentifier!
        delete(serviceIdentifier, key: .Username)
        delete(serviceIdentifier, key: .PlayerId)
    }
    
    /**
    * Internal methods for querying the keychain.
    */
    private class func save(service: NSString, key: KeychainKey, value: NSString) {
        NSLog("About to save \(key) in Keychain")
        let valueData: NSData = value.dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: false)!
        let keyData: NSData = keyAsString(key).dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: false)!
        
        // Instantiate a new default keychain query
        var keychainQuery: NSMutableDictionary = NSMutableDictionary(objects: [kSecClassGenericPasswordValue, service, keyData, valueData], forKeys: [kSecClassValue, kSecAttrServiceValue, kSecAttrAccountValue, kSecValueDataValue])
        NSLog("Deleting exising items in Keychain")
        // Delete any existing items
        SecItemDelete(keychainQuery as CFDictionaryRef)
        
        // Add the new keychain item
        var status: OSStatus = SecItemAdd(keychainQuery as CFDictionaryRef, nil)
        NSLog("Item \(key) saved in Keychain. Status: \(status)")
    }
    
    private class func load(service: NSString, key: KeychainKey) -> NSString? {
        // Instantiate a new default keychain query
        // Tell the query to return a result
        // Limit our results to one item
        let keyString = keyAsString(key) as String;
        NSLog("Loading \(keyString) from Keychain")
        let keychainQuery: NSMutableDictionary = NSMutableDictionary(objects: [kSecClassGenericPasswordValue, service, keyString, kCFBooleanTrue, kSecMatchLimitOneValue], forKeys: [kSecClassValue, kSecAttrServiceValue, kSecAttrAccountValue, kSecReturnDataValue, kSecMatchLimitValue])
        
        var dataTypeRef :Unmanaged<AnyObject>?
        NSLog("About to call SecItemCopyMatching with key chain query \(keychainQuery)")
        // Search for the keychain items
        
        
        var contentsOfKeychain: NSString?
        
        var result: AnyObject?
        var status = withUnsafeMutablePointer(&result) {
            SecItemCopyMatching(keychainQuery, UnsafeMutablePointer($0))
        }
        
        if status == errSecSuccess {
            let data = result as NSData?
            contentsOfKeychain = NSString(data: data!, encoding: NSUTF8StringEncoding)
        }
        
        return contentsOfKeychain
    }
    
    private class func delete(service: String, key: KeychainKey) {
        let keyData: NSData = keyAsString(key).dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: false)!
        var keychainQuery: NSMutableDictionary = NSMutableDictionary(objects: [kSecClassGenericPasswordValue, service, keyData], forKeys: [kSecClassValue, kSecAttrServiceValue, kSecAttrAccountValue])
        
        // Delete any existing items
        SecItemDelete(keychainQuery as CFDictionaryRef)
    }
    
    private class func keyAsString(key: KeychainKey) -> String {
        switch key {
        case .Username: return "username";
        case .PlayerId: return "id";
        }
    }
}