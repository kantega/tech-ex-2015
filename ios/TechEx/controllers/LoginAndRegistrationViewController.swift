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

    @IBOutlet weak var disclaimer: UITextView!
    
    //@IBOutlet weak var welcomeView: UIView!
    //@IBOutlet weak var welcomeLabel: UILabel!
    @IBOutlet weak var registrationView: UIView!
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
                    println("Error when registering user: \(error)");
                } else {
                    let d = JSON(j!);
                    let playerId = d["id"].string!
                    let nick = d["nick"].string!
                    println("PlayerId: \(playerId), nick: \(nick)")
                
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
            println("User is logged in as \(nick). Displaying welcome message.");
            showWelcomeMessage();
        } else {
            println("User is not logged in. Displaying registration form.");
            showRegistrationView();
        }
        super.viewWillAppear(animated);
    }
    
    override func viewDidAppear(animated: Bool) {
//        if (!welcomeView.hidden) {
//            showQuests();
//        }
    }
    
    override func viewDidLoad() {
         self.view.backgroundColor = UIColor.clearColor()
    }
    
    
    func loadCredentialsFromKeychain() {
        self.id = KeychainService.load(.PlayerId)
        self.nick = KeychainService.load(.Username)
    }
    

    func isLoggedIn() -> Bool {
        return id != nil;
    }
    
    func showWelcomeMessage() {
//        welcomeLabel.text = "Velkommen tilbake, \(nick)!";
//        welcomeView.hidden = false;
    }
    
    func showRegistrationView() {
//        welcomeView.hidden = true;
        registrationView.hidden = false;
        nickTextField.attributedPlaceholder = NSAttributedString(string:"Nickname...",
            attributes:[NSForegroundColorAttributeName: UIColor(red: CGFloat(197/255.0), green: CGFloat(49/255.0), blue: CGFloat(147/255.0), alpha: 1.0)])
        disclaimer.textColor = UIColor.whiteColor()
    }
    
    func showQuests() {
        println("ShowingQuests, i.e. performing segue with identifier ShowQuests");
        self.performSegueWithIdentifier("ShowQuests", sender: nil);
    }
    


    

}

