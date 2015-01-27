//
//  ViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 22/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class LoginAndRegistrationViewController: UIViewController {

    //TODO: Last fra config
    let baseApiUrl = "http://localhost:9000";
    var nick:String!
    var token:String!;

    @IBOutlet weak var welcomeView: UIView!
    @IBOutlet weak var registrationView: UIView!
    @IBOutlet weak var welcomeLabel: UILabel!
    @IBOutlet weak var nickTextField: UITextField!

    
    @IBAction func registerUser() {
        // TODO: Validate inputs
        self.nick = nickTextField.text;
        //TODO: Show spinner.
        request(.PUT, "\(baseApiUrl)/player/\(nick)")
            .responseJSON { (req, resp, JSON, error) in
                let d = JSON as NSDictionary;
                let idToken = d.objectForKey("id") as String
                let nick = d.objectForKey("nick") as String
                NSLog("Token: \(idToken), nick: \(nick)")
                KeychainService.save(.Username, value: nick)
                KeychainService.save(.Token, value: idToken);
                self.showQuests();
        }

    }

    
    override func viewWillAppear(animated: Bool) {
        loadCredentialsFromKeychain();
        if (isLoggedIn()) {
            NSLog("User is logged in as \(nick). Displaying welcome message.");
            showWelcomeMessage();
        } else {
            NSLog("User is not logged in. Displaying registration form.");
            showRegistrationView();
        }
        super.viewWillAppear(animated);
    }
    
    override func viewDidAppear(animated: Bool) {
        if (!welcomeView.hidden) {
            showQuests();
        }
    }
    
    func loadCredentialsFromKeychain() {
        self.token = KeychainService.load(.Token)
        self.nick = KeychainService.load(.Username)
    }
    

    func isLoggedIn() -> Bool {
        return token != nil;
    }
    
    func showWelcomeMessage() {
        welcomeLabel.text = "Velkommen tilbake, \(nick)!";
        welcomeView.hidden = false;
    }
    
    func showRegistrationView() {
        welcomeView.hidden = true;
        registrationView.hidden = false;
    }
    
    func showQuests() {
        NSLog("ShowingQuests, i.e. performing segue with identifier ShowQuests");
        self.performSegueWithIdentifier("ShowQuests", sender: nil);
    }
    

}

