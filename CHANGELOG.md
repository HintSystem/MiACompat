* Add ghost seek ping breadcrumbs
  * When you get a ghost seek ping, a box will appear in that location to indicate which locations you have tried before.
  The closer the ping is to the praying skeleton the greener the box will be
* Add ghost seek ping distance hints
* Added command:
  * `/miacompat`
    * `clear_breadcrumbs` - clear ghost seek breadcrumbs
* Added config options:
  * Ghost Seek
    * Distance Hints - displays distance in action bar<br>
     *example: `du dum (75m ± 25m)`*<br>
     *default: `ON`*
    * Breadcrumb Duration - how long breadcrumbs are displayed, set to 0 to disable <br>
     *default: `300s (5 min)`* 
* Hopefully fix an issue where waypoints could get messed up if the mod has an unexpected crash. So far has only happened during testing