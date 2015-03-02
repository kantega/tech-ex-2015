//
//  QuestsViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 27/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class QuestsTableViewController: UITableViewController{
    
    @IBOutlet weak var helpButton: UIBarButtonItem!

    let questCellIdentifier = "QuestPrototypeCell"
    var quests = Array<Quest>()
    var baseApiUrl = ""
    
    
    deinit {
        NSNotificationCenter.defaultCenter().removeObserver(self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.baseApiUrl = NSBundle.mainBundle().objectForInfoDictionaryKey("serverUrl") as String
        
        self.setTechExBackgroundImage()

        NSNotificationCenter.defaultCenter().addObserver(self, selector: Selector("showAwardView:"), name: "badgeReceived", object: nil)
        
        self.refreshControl = UIRefreshControl()
        self.refreshControl?.tintColor = UIColor.whiteColor()
        self.refreshControl!.addTarget(self, action: "loadQuests", forControlEvents: UIControlEvents.ValueChanged)
        self.tableView.addSubview(refreshControl!)
        
        self.helpButton.enabled = false
        self.helpButton.tintColor = UIColor.clearColor()
        
        self.navigationItem.titleView = UIImageView(image: UIImage(named: "NavbarLogo"))
        
        // Hide empty table rows
        self.tableView.tableFooterView = UIView(frame: CGRectZero)
        self.navigationController?.view.backgroundColor = UIColor.clearColor()
        
        NSLog("QuestsTableViewController.viewDidLoad() bootstrapping complete. About to load initial data.")
        self.loadQuests()
        self.loadHelpText()
        self.startDetectingBeacons()
    }

    
    func loadQuests() {
        LoadingOverlay.shared.showOverlay(self.view)
        NSLog("Loading playerId from KeyChain")
        let playerId = KeychainService.load(.PlayerId)!;
        NSLog("PlayerId is \(playerId)")
        let requestUrl = "\(baseApiUrl)/quests/player/\(playerId)"
        NSLog("About to load quests for player from resource \(requestUrl)")
        
        request(.GET, requestUrl)
            .responseJSON { (req, resp, j, error) in
                if resp == nil || resp?.statusCode != 200 {
                    Alert.shared.showAlert("Unable to load quests. Please try again later.", title: "Error", buttonText: "OK", parent: self);
                    println("Error when loading quests: \(error)");
                } else {
                    let userQuests = JSON(j!)
                    self.quests = Array<Quest>()
                    for (index: String, quest: JSON) in userQuests {                        
                        let q = Quest()
                        q.title = quest["title"].string!
                        q.desc = quest["desc"].string!
                        q.visibility = quest["visibility"].string!
                        let achievements = quest["achievements"].array!
                        for a in achievements {
                            let ach = Achievement()
                            ach.id = a["id"].string!
                            ach.title = a["title"].string!
                            ach.desc = a["desc"].string!
                            ach.achieved = a["achieved"].bool!
                            q.achievements.append(ach)
                        }
                        self.quests.append(q)
                    }
                    self.tableView!.reloadData()
                }
                self.refreshControl?.endRefreshing()
                LoadingOverlay.shared.hideOverlayView();
        }
    }

    func loadHelpText() {
        request(.GET,"\(baseApiUrl)/text/help/ios")
            .responseString { (req, resp, text, error) in
                if (error == nil && text != nil && !text!.isEmpty) {
                    self.helpButton.enabled = true
                    self.helpButton.tintColor = UIColor.whiteColor()
                }
        }
    }
    
    func startDetectingBeacons() {
        let appDelegate = UIApplication.sharedApplication().delegate as AppDelegate
        appDelegate.startTrackingIBeacons()
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return quests.count
    }
    
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(questCellIdentifier, forIndexPath: indexPath) as QuestTableViewCell
        return configureTableCell(cell, indexPath: indexPath)
    }

    func configureTableCell(cell: QuestTableViewCell, indexPath: NSIndexPath) -> UITableViewCell {
        let quest = self.quests[indexPath.row]
        cell.titleLabel.text = quest.title
        cell.titleLabel.textColor = UIColor.whiteColor()
        cell.backgroundColor = UIColor.clearColor()
        
        // Background colour for the active (selected) row
        let backgroundView = UIView(frame: cell.frame);
        backgroundView.backgroundColor = UIColor(red: 0/255.0, green:0/255.0, blue:0/255.0, alpha: 0.1);
        cell.selectedBackgroundView = backgroundView;
        
        cell.badgesView.totalBadges = quest.achievements.count
        cell.badgesView.achievedBadges = quest.achievements.filter{ $0.achieved }.count
        return cell
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        return 80
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if segue.identifier == "QuestDetail" {
            let questDetailViewController = segue.destinationViewController as QuestTableViewController
            let indexPath = self.tableView.indexPathForSelectedRow()!
            questDetailViewController.quest = self.quests[indexPath.row]
        }
    }

    


}
