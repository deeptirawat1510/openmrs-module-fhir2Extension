openmrs-module-fhir2Extension
==========================

This module's objective is to provide specific implementation extensions. To start with
1. DiagnosticReport - using the obs model as basis of DiagnosticReport (compatible with the datastructures used by EMRAPI and Bahmni)
2. FHIR Export - Exports Patient data in FHIR format


Description
-----------


Building from Source
--------------------
You will need to have Java 1.8+ and Maven 2.x+ installed.  Use the command 'mvn package' to 
compile and package the module.  The .omod file will be in the omod/target folder.


Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application 
Data Directory that the running openmrs is currently using.)  After putting the file in there 
simply restart OpenMRS/tomcat and the module will be loaded and started.
