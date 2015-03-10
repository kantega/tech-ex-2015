//
//  AppDelegate.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 22/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit
import CoreLocation


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, CLLocationManagerDelegate  {

    var window: UIWindow?

    var baseApiUrl = ""
    var playerId = "";
    let locationManager = CLLocationManager()
   
    var beaconList = BeaconList()
   
    
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

        return true
    }
    
    
    // IBEACONS
    
    func startDetectingBeacons(initializingView: UIViewController) {

        baseApiUrl = NSBundle.mainBundle().objectForInfoDictionaryKey("serverUrl") as String
        locationManager.delegate = self
        playerId = KeychainService.load(.PlayerId)!
        if (CLLocationManager.authorizationStatus() != CLAuthorizationStatus.Authorized) {
            // The 'when in use' authorization only allows you to use location services while the app is in the foreground,
            // whereas the 'always' authorization lets you access location services at any time, even waking up and starting the app in response to some event
            locationManager.requestAlwaysAuthorization()
        }

        request(.GET, "\(baseApiUrl)/beaconregions")
            .responseJSON { (req, resp, j, error) in
                if error != nil || resp == nil || resp?.statusCode != 200 {
                    Alert.shared.showAlert("Error when communicating with server. Please try again later", title: "Error", buttonText: "OK", parent: initializingView);
                    NSLog("Error when fetching beacon regions: \(error). HTTP response: \(resp)");
                    
                } else {
                    let r = JSON(j!)
                    let numberOfRegions = r["numberOfRegions"].intValue
                    NSLog("Setting up \(numberOfRegions) beacon regions")
                    for i in 1...numberOfRegions {
                        let region = CLBeaconRegion(proximityUUID: NSUUID(UUIDString: "f7826da6-4fa2-4e98-8024-bc5b71e0893e"), major: CLBeaconMajorValue(i), identifier: "Region \(i)")
                        self.locationManager.startMonitoringForRegion(region)
                        self.locationManager.startRangingBeaconsInRegion(region)
                    }
                }
        }

    }
    
    
    func locationManager(manager: CLLocationManager!, didEnterRegion region: CLRegion!) {
        NSLog("Device entered \(region.identifier). Inside regions \(beaconList.currentRegions()) before entering.")
        locationManager.startRangingBeaconsInRegion(region as CLBeaconRegion)
    }
    
    func locationManager(manager: CLLocationManager!, didExitRegion region: CLRegion!) {
        let beaconRegion = region as CLBeaconRegion
        NSLog("Device left \(region.identifier).  Inside regions \(beaconList.currentRegions()) before leaving.")
        locationManager.stopRangingBeaconsInRegion(beaconRegion)
        beaconList.remove(beaconRegion.major.integerValue)
        if beaconList.hasLeftAllRegions() {
            NSLog("Device has left all regions. Sending exit message.")
            updateLocation(["activity": "exit"])
        }
    }
    
    func locationManager(manager: CLLocationManager!, didRangeBeacons beacons: [AnyObject]!, inRegion region: CLBeaconRegion!) {
        var clBeacons = beacons.map({$0 as CLBeacon}).filter({$0.proximity != CLProximity.Unknown })

        if (clBeacons.count > 0) {
            let currentNearest = beaconList.nearest()
            beaconList.insert(clBeacons)
            let newNearest = beaconList.nearest()

            let minorIds = clBeacons.map {$0.minor}
            let nearestIds = beaconList.beacons.map{$0.minor}
            //NSLog("Location update: Found beacons \(minorIds) in region \(region!.major). Beacons in sight now: \(nearestIds).")
            
            if (currentNearest?.minor != newNearest?.minor || currentNearest?.proximity.rawValue > newNearest!.proximity.rawValue) {// Location has changed
                NSLog("Nearest beacon has changed. Current nearest is \(newNearest!.minor)")
                let parameters = [
                    "major": "\(newNearest!.major)",
                    "minor": "\(newNearest!.minor)",
                    "proximity": "\(newNearest!.proximity.rawValue)",
                    "activity": "enter"
                ];
                updateLocation(parameters)
            }
        }
    }
    
    
    func updateLocation(parameters: [String: AnyObject]) {
        NSLog("Location update: Sending data to server. Parameters: \(parameters)")
        request(.POST, "\(baseApiUrl)/location/\(playerId)", parameters: parameters, encoding: .JSON)
            .responseString { (req, resp, s, error) in
                if error != nil {
                    NSLog("Location update: Error \(error)");
                } else {
                    NSLog("Location update: Response code \(resp!.statusCode). Response body: \(s!)")
                }
        }
    }
    
    
    
    // NOTIFICATIONS

    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        let token = tokenAsString(deviceToken)
        NSUserDefaults.standardUserDefaults().setValue(token, forKey: "deviceToken")
        NSLog("Device token for notifications: \(token)")
    }
    
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        NSLog("Failed to register for notifications. Error: \(error)")
    }
    
    func application(application: UIApplication, didReceiveRemoteNotification userInfo: [NSObject : AnyObject]) {
        NSNotificationCenter.defaultCenter().postNotification(NSNotification(name: "badgeReceived", object: nil, userInfo: userInfo))
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
        NSLog("App did enter background")
    }

    func applicationWillEnterForeground(application: UIApplication) {
        NSLog("App did enter foreground")
    }

    func applicationDidBecomeActive(application: UIApplication) {
        
    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }


}
