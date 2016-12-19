[Smap fieldTask](http://www.smap.com.au) 
======

This was the second version of fieldTask.  It has now been replaced by version 4 and is being maintained in a new repository [https://github.com/smap-consulting/fieldTask4](https://github.com/smap-consulting/fieldTask4)

fieldTask is an Android client for Smap Server that extends [odkCollect](http://opendatakit.org/use/collect/) with Task Management functionality. It depends on a modified
version of odkCollect referenced as a library.

Follow the latest news about Smap on our [blog](http://blog.smap.com.au) and on twitter [@dgmsot](https://twitter.com/dgmsot).

Frequently Asked Questions
---------------------------
##### How to install and run
* Install Android Studio
* In Android Studio open the SDK manager (from the tools menu)
* Under "Extras" install:
    * Android Support Repository
    * Android Support Library
    * Google Play Services
    * Google Repository
* Clone as a GIT project into Android Studio
* Select fieldTask and run as an Android application

Instructions on installing a Smap server can be found in the operations manual [here](http://www.smap.com.au/downloads.shtml)

Task Management 
---------------

A user of fieldTask can be assigned tasks to complete as per this [video](http://www.smap.com.au/taskManagement.shtml). 

Future Directions
-----------------

Smap Server supports completing surveys using web forms as well as on Android devices.  This isn't as mature as as the Android client however it does already support an updated version of the Task Management API. 

##### Get existing survey data as an XForm instance XML file
https://hostname/instanceXML/{survey id}/0?datakey={key name}&datakeyvalue={value of key}

##### Update existing results
https://{hostname}/submission/{instanceid}

Note the instance id of the existing data is included in the instanceXML.  It should be replaced with a new instance id before the results are submitted. However the instance id of the data to be replaced needs to be included in teh submission URL.

This API allows you to maintain data using surveys. In the following video the data is published on a map, however it could also be published in a table as a patient registry or list of assets. fieldTask needs to be customised to access these links using the data keys in a similar way to web forms.

[![ScreenShot](http://img.youtube.com/vi/FUNPOmMnt1I/0.jpg)](https://www.youtube.com/watch?v=FUNPOmMnt1I)

Development
-----------
* Code contributions are very welcome. 
* [Issue Tracker](https://github.com/nap2000/fieldTask2/issues)

Acknowledgements
----------------

This project includes:
* the odkCollect Library of (http://opendatakit.org/) from the University of Washington
* the Android SDK from [MapBox] (https://www.mapbox.com/)
