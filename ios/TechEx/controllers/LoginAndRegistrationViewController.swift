//
//  ViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 22/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class LoginAndRegistrationViewController: UIViewController {

   

    let baseApiUrl = Config.get("ServerUrl")
    var nick:String!
    var id:String!

    @IBOutlet weak var welcomeView: UIView!
    @IBOutlet weak var registrationView: UIView!
    @IBOutlet weak var welcomeLabel: UILabel!
    @IBOutlet weak var nickTextField: UITextField!

    
    @IBAction func registerUser() {
        if (!nickTextField.hasText()) {
            Alert.shared.showAlert("Nick cannot be empty", title: nil, buttonText: "OK", parent: self);
            return
        }

        
        self.nick = nickTextField.text;
        
        LoadingOverlay.shared.showOverlay(self.view);
        
        request(.PUT, "\(baseApiUrl)/player/\(nick)")
            .responseJSON { (req, resp, j, error) in
                if error != nil {
                    Alert.shared.showAlert("Unable to register user. Please try again later.", title: "Error", buttonText: "OK", parent: self);
                    NSLog("Error when registering user: \(error)");
                } else {
                    let d = JSON(j!);
                    let playerId = d["id"].string!
                    let nick = d["nick"].string!
                    NSLog("PlayerId: \(playerId), nick: \(nick)")
                
                    KeychainService.save(.Username, value: nick)
                    KeychainService.save(.PlayerId, value: playerId)
                    self.showQuests()
                }
                LoadingOverlay.shared.hideOverlayView();
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
        self.id = KeychainService.load(.PlayerId)
        self.nick = KeychainService.load(.Username)
    }
    

    func isLoggedIn() -> Bool {
        return id != nil;
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

