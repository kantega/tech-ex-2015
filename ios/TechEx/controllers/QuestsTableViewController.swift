//
//  QuestsViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 27/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class QuestsTableViewController: UITableViewController{
    

    let questCellIdentifier = "QuestPrototypeCell"
    let baseApiUrl = Config.get("ServerUrl")
    var quests = Array<Quest>()
    
    
    override func viewDidLoad() {
//        //Move the content down to prevent it from being behind the status bar.
//        self.tableView.contentInset = UIEdgeInsetsMake(20, 0, 0, 0);
        loadInitialData()
        self.startDetectingBeacons()
        
        self.refreshControl = UIRefreshControl()
        //self.refreshControl!.attributedTitle = NSAttributedString(string: "Pull to refersh")
        self.refreshControl!.addTarget(self, action: "loadInitialData", forControlEvents: UIControlEvents.ValueChanged)
        self.tableView.addSubview(refreshControl!)
        
        super.viewDidLoad()
    }

    
    func loadInitialData() {
        LoadingOverlay.shared.showOverlay(self.view)
        let playerId = KeychainService.load(.PlayerId)!;
        
        request(.GET, "\(baseApiUrl)/quests/player/\(playerId)")
            .responseJSON { (req, resp, j, error) in
                if error != nil {
                    Alert.shared.showAlert("Unable to load quests. Please try again later.", title: "Error", buttonText: "OK", parent: self);
                    NSLog("Error when loading quests: \(error)");
                } else {
                    let userQuests = JSON(j!)
                    self.quests = Array<Quest>()
                    for (index: String, quest: JSON) in userQuests {
                        let q = Quest()
                        let title = quest["title"].string!
                        NSLog("Adding quest with title \(title)")
                        q.title = title
                        q.desc = quest["desc"].string!
                        self.quests.append(q)
                        self.tableView!.reloadData()

                    }
                }
                self.refreshControl?.endRefreshing()
                LoadingOverlay.shared.hideOverlayView();
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
        cell.descriptionLabel.text = quest.desc
        return cell
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        let sizingCell = tableView.dequeueReusableCellWithIdentifier(questCellIdentifier) as QuestTableViewCell
        
        self.configureTableCell(sizingCell, indexPath: indexPath)
        sizingCell.setNeedsLayout()
        sizingCell.layoutIfNeeded()
        
        let size = sizingCell.contentView.systemLayoutSizeFittingSize(UILayoutFittingCompressedSize)
        return size.height + 1.0
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if segue.identifier == "QuestDetail" {
            let questDetailViewController = segue.destinationViewController as UIViewController
            let indexPath = self.tableView.indexPathForSelectedRow()!
            let destinationTitle = self.quests[indexPath.row].title
            questDetailViewController.title = destinationTitle
        }
    }

    
    /*
    // Override to support conditional editing of the table view.
    override func tableView(tableView: UITableView, canEditRowAtIndexPath indexPath: NSIndexPath) -> Bool {
    // Return NO if you do not want the specified item to be editable.
    return true
    }
    */
    
    /*
    // Override to support editing the table view.
    override func tableView(tableView: UITableView, commitEditingStyle editingStyle: UITableViewCellEditingStyle, forRowAtIndexPath indexPath: NSIndexPath) {
    if editingStyle == .Delete {
    // Delete the row from the data source
    tableView.deleteRowsAtIndexPaths([indexPath], withRowAnimation: .Fade)
    } else if editingStyle == .Insert {
    // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }
    }
    */
    
    /*
    // Override to support rearranging the table view.
    override func tableView(tableView: UITableView, moveRowAtIndexPath fromIndexPath: NSIndexPath, toIndexPath: NSIndexPath) {
    
    }
    */
    
    /*
    // Override to support conditional rearranging of the table view.
    override func tableView(tableView: UITableView, canMoveRowAtIndexPath indexPath: NSIndexPath) -> Bool {
    // Return NO if you do not want the item to be re-orderable.
    return true
    }
    */


    


}
