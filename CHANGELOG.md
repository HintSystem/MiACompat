* Add ghost seek ping breadcrumbs
  * When you get a ghost seek ping, a box will appear in that location to indicate which locations you have tried before
  * The closer the ping is to the praying skeleton the greener the breadcrumb will be
  * Only visible if ghost seek is in a passive slot
* Add ghost seek ping distance hints
* Added command:
  * `/miacompat`
    * `clear_breadcrumbs` - clear ghost seek breadcrumbs
* Added config options:
  * Ghost Seek
    * Distance Hints - displays distance in action bar<br>
     *example: `dum tick (100-150 blocks)`*<br>
     *default: `ON`*
    * Breadcrumb Duration - how long breadcrumbs are displayed, set to 0 to disable <br>
     *default: `300s (5 min)`* 
* Fix bonfire tracking for new server version (bonfire id changed)
* Tracked bonfire status now updates whenever a bonfire loads
  * Fixes bonfires not being unlinked when punched or destroyed in front of the player
* Fixed rare issue where waypoints could get saved with incorrect coordinates if the mod crashed unexpectedly
  * So far only observed during testing