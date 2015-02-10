//
//  QuestsViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 27/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class QuestsTableViewController: UITableViewController {
    
    //TODO: Last fra config
    let baseApiUrl = "http://localhost:9000";
    
    var quests = NSMutableArray()
    
    override func viewDidLoad() {
        //Move the content down to prevent it from being behind the status bar.
        self.tableView.contentInset = UIEdgeInsetsMake(20, 0, 0, 0);
        loadInitialData()
        super.viewDidLoad()
    }

    
    func loadInitialData() {
        LoadingOverlay.shared.showOverlay(self.view)
        let nick = KeychainService.load(.Username)!;
        
        request(.GET, "\(baseApiUrl)/quests/\(nick)")
            .responseJSON { (req, resp, j, error) in
                if error != nil {
                    Alert.shared.showAlert("Unable to load quests. Please try again later.", title: "Error", buttonText: "OK", parent: self);
                    NSLog("Error when loading quests: \(error)");
                } else {
                    let quests = JSON(j!)
                    for (index: String, quest: JSON) in quests {
                        let q = Quest()
                        let title = quest["title"].string!
                        NSLog("Adding quest with title \(title)")
                        q.title = title
                        self.quests.addObject(q)
                        self.tableView!.reloadData()
                    }
                }
                LoadingOverlay.shared.hideOverlayView();
        }
    }

    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return quests.count
    }
    
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier("QuestPrototypeCell", forIndexPath: indexPath) as UITableViewCell
        let quest = self.quests.objectAtIndex(indexPath.row) as Quest
        cell.textLabel?.text = quest.title
        return cell
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
    
    /*
    // MARK: - Navigation
    
    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
    }
    */
    


}
