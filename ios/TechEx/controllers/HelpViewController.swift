//
//  HelpViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 02/03/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class HelpViewController: UIViewController {

    let bodyStart =  "<!doctype html><html><head></head><body style=\"background-color: transparent; color: #fff; font-family: Verdana, Arial, sans-serif;\">"
    let bodyEnd = "<body></html>"
    
    var helpText:String = "" {
        didSet {
            setHtmlText()
        }
    }

    @IBOutlet weak var textView: UIWebView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.navigationItem.titleView = UIImageView(image: UIImage(named: "NavbarLogo"))
        NSLog("Loading helpView. Help text: \(helpText)")
        setTechExBackgroundImage()
        textView.backgroundColor = UIColor.clearColor()
        textView.opaque = false
        self.loadHelpText()
    }
    
    
    func loadHelpText() {
        LoadingOverlay.shared.showOverlay(self.view)
        let baseApiUrl = NSBundle.mainBundle().objectForInfoDictionaryKey("serverUrl") as String
        request(.GET,"\(baseApiUrl)/text/help/ios")
            .responseString { (req, resp, text, error) in
                if (error == nil && text != nil && !text!.isEmpty) {
                    self.helpText = text!
                } else {
                    self.helpText = "Help is currently unavailable"
                }
                LoadingOverlay.shared.hideOverlayView();
        }
    }
    

    func setHtmlText() {
        let mergedText = bodyStart + helpText + bodyEnd
        textView.loadHTMLString(mergedText, baseURL: nil)
    }
    
}
