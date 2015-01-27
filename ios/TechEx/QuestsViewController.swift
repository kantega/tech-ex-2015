//
//  QuestsViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 27/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class QuestsViewController: UIViewController {

    @IBOutlet weak var usernameLabel: UILabel!
    
    override func viewDidLoad() {
        var nick = KeychainService.load(.Username)
        usernameLabel.text = "Du er logget inn som \(nick!)"
        super.viewDidLoad()

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    

}
