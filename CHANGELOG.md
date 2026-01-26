* Changed commands:
  * `/miacompat`
    * `breadcrumbs`
      * `add` - specify ping length and add breadcrumb to test visuals
      * `clear` - clear ghost seek breadcrumbs *(previously `clear_breadcrumbs`)*
* Added config options:
  * Ghost Seek
    * Clear Breadcrumbs On Find - clears breadcrumbs after hitting a praying skeleton
    
      *default: `ON`*
    * Breadcrumb Visual Type
    * Breadcrumb Size
    * Breadcrumb Distance Scale - scales breadcrumb size based on distance to the praying skeleton
      <br>
      0 = no scaling \
      \+ = bigger when further away \
      \- = bigger when closer
    * Breadcrumb Opacity