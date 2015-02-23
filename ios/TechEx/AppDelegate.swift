//
//  AppDelegate.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 22/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?
    let locationService = LocationService()
   
    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        // Delete keychain data from previous installs.
        if (NSUserDefaults.standardUserDefaults().objectForKey("appInitialized") == nil) {
            KeychainService.deleteAll()
            NSUserDefaults.standardUserDefaults().setValue(true, forKey: "appInitialized")
        }

        let settings = UIUserNotificationSettings(forTypes: .Alert, categories: nil)
        UIApplication.sharedApplication().registerUserNotificationSettings(settings)
        UIApplication.sharedApplication().registerForRemoteNotifications();
        println("Has registered for remote notifications")

        
        //Transparent navigation bar
        UINavigationBar.appearance().setBackgroundImage(UIImage(), forBarMetrics: UIBarMetrics.Default)
        UINavigationBar.appearance().shadowImage = UIImage()
        UINavigationBar.appearance().translucent = true
        // Link and title text color
        UINavigationBar.appearance().tintColor = UIColor.whiteColor()
        
        //TODO: Ta vekk!! Kun for testing
        sleep(2);
        return true
    }
    
    
    func startTrackingIBeacons() {
        println("Initiating LocationService")
        locationService.startDetectingBeacons();
    }

    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        let token = tokenAsString(deviceToken)
        NSUserDefaults.standardUserDefaults().setValue(token, forKey: "deviceToken")
        NSLog("Device token for notifications: \(token)")
    }
    
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        NSLog("Failed to register for notifications. Error: \(error)")
    }
    
    func application(application: UIApplication, didReceiveRemoteNotification userInfo: [NSObject : AnyObject]) {
        NSNotificationCenter.defaultCenter().postNotification(NSNotification(name: "badgeReceived", object: nil))
    }

    func tokenAsString(token: NSData) -> String{
        let tokenChars = UnsafePointer<CChar>(token.bytes)
        var tokenString = NSMutableString()
        
        for var i = 0; i < token.length; i++ {
            tokenString.appendFormat("%02.2hhx", tokenChars[i])
        }
        
        return tokenString
    }

    func applicationWillResignActive(application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(application: UIApplication) {
        NSLog("applicationWillEnterForeground: Start ranging beacons in region")
        locationService.startRangingBeaconsInRegion()
    }

    func applicationDidBecomeActive(application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }


}
