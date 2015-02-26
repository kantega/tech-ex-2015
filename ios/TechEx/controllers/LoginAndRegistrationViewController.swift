//
//  ViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 22/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class LoginAndRegistrationViewController: UIViewController {

   

    var nick:String!
    var id:String!

    
    @IBOutlet weak var welcomeView: UIView!
    @IBOutlet weak var welcomeLabel: UILabel!
    @IBOutlet weak var registrationView: UIView!
    @IBOutlet weak var nickTextField: UITextField!
    @IBOutlet weak var disclaimer: UITextView!

    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.view.backgroundColor = UIColor(patternImage: UIImage(named: "Background")!)
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated);
        loadCredentialsFromKeychain();
        if (isLoggedIn()) {
            println("User is logged in as \(nick). Displaying welcome message.");
            showWelcomeMessage();
        } else {
            println("User is not logged in. Displaying registration form.");
            showRegistrationView();
        }
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        if (!welcomeView.hidden) {
            NSTimer.scheduledTimerWithTimeInterval(2.0, target: self, selector: Selector("showQuests"), userInfo: nil, repeats: false)
        }
    }
    
    
    @IBAction func registerUser() {
        if (!nickTextField.hasText()) {
            Alert.shared.showAlert("Nick cannot be empty", title: nil, buttonText: "OK", parent: self);
            return
        }
        //Trim input value
        self.nick = nickTextField.text.stringByTrimmingCharactersInSet(NSCharacterSet.whitespaceCharacterSet())

        let tokenData = NSUserDefaults.standardUserDefaults().objectForKey("deviceToken") as? String
        let deviceToken = (tokenData != nil) ? "\(tokenData!)" : ""

        LoadingOverlay.shared.showOverlay(self.view);
        
        let parameters:[String:AnyObject] = [
            "nick": self.nick,
            "platform": [
                "type": "ios",
                "deviceToken": deviceToken
            ]
        ]

        let baseApiUrl = NSBundle.mainBundle().objectForInfoDictionaryKey("serverUrl") as String
        NSLog("POSTing player with parameters \(parameters)")
        
        request(.POST, "\(baseApiUrl)/players", parameters: parameters, encoding: .JSON)
            .responseJSON { (req, resp, j, error) in
                NSLog("Register user response received.")
                if error != nil || resp == nil || resp?.statusCode != 200 {
                    Alert.shared.showAlert("Unable to register user. Please try again later.", title: "Error", buttonText: "OK", parent: self);
                    NSLog("Error when registering user: \(error)");
                } else {
                    let d = JSON(j!);
                    let playerId = d["id"].string!
                    let nick = d["nick"].string!
                    
                    KeychainService.save(.Username, value: nick)
                    KeychainService.save(.PlayerId, value: playerId)
                    NSLog("Successfully registered player with playerId: \(playerId), nick: \(nick)")
                    self.showQuests()
                }
                LoadingOverlay.shared.hideOverlayView();
        }

    }

    // "Done" keyboard button is pressed
    @IBAction func doneEnteringNick() {
        self.resignFirstResponder()
        registerUser()
    }
    
    // Tap outside the keyboard when entering nick. Close the keyboard.
    @IBAction func backgroundTap() {
        nickTextField.resignFirstResponder()
    }
    
    func loadCredentialsFromKeychain() {
        self.id = KeychainService.load(.PlayerId)
        self.nick = KeychainService.load(.Username)
    }
    

    func isLoggedIn() -> Bool {
        return id != nil;
    }
    
    func showWelcomeMessage() {
        welcomeLabel.text = "Velkommen tilbake, \(nick)!";
        welcomeLabel.textColor = UIColor.whiteColor()
        welcomeView.hidden = false;
    }
    
    func showRegistrationView() {
        welcomeView.hidden = true;
        registrationView.hidden = false;
        nickTextField.attributedPlaceholder = NSAttributedString(string:"Nickname...",
            attributes:[NSForegroundColorAttributeName: UIColor(red: CGFloat(197/255.0), green: CGFloat(49/255.0), blue: CGFloat(147/255.0), alpha: 1.0)])
        disclaimer.textColor = UIColor.whiteColor()
        disclaimer.backgroundColor = UIColor(red: CGFloat(15/255.0), green: CGFloat(134/255.0), blue: CGFloat(128/255.0), alpha: 0.7)
    }
    
    func showQuests() {
        NSLog("ShowingQuests, i.e. performing segue with identifier ShowQuests");
        self.performSegueWithIdentifier("ShowQuests", sender: nil);
    }
    


    

}

