//
//  Alert.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 28/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class Alert: NSObject {
   
    class var shared: Alert {
        struct Static {
            static let instance: Alert = Alert()
        }
        return Static.instance
    }
    
    func showAlert(message: String, title: String?, buttonText: String, parent: UIViewController) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .Alert)
        let defaultAction = UIAlertAction(title: buttonText, style: .Default, handler: nil)
        alertController.addAction(defaultAction)        
        parent.presentViewController(alertController, animated: true, completion: nil)
    }
}
