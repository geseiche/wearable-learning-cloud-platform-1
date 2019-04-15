node {
  def mvnHome
  
  try {
  	  notifyBuild('STARTED')
  
	  stage('Preparation') {
	      git 'https://github.com/wearable-learning/wearable-learning-cloud-platform.git'
	      mvnHome = tool 'M3'
	  }
	  stage('Build') {
	      sh "'${mvnHome}/bin/mvn' clean install -Pbuild -DskipTests"
	  }
	  stage('Unit Testing') {
	  	 if(params.unit) {
	      sh "'${mvnHome}/bin/mvn' test -Dwdm.chromeDriverVersion=73.0.3683.68"
	     }
	  }
	  stage('Integration Testing') {
	  	if(params.integration) {
	  	  sh "'${mvnHome}/bin/mvn' -f WLCPFrontEnd/pom.xml test -Pintegration-tests -Djava.io.tmpdir=/var/lib/jenkins/workspace/WLCP/temp"
	  	}
	  }
	  stage('Publish Test Results') {
	  	if(params.unit) {
	      junit "WLCPDataModel/target/surefire-reports/*.xml"
	      junit "WLCPFrontEnd/target/surefire-reports/*.xml"
	      junit "WLCPGameServer/target/surefire-reports/*.xml"
	      junit "WLCPWebApp/target/surefire-reports/*.xml"
	    }
	  }
	  stage('Publish Artifacts') {
	  	if(params.publish) {
	       withCredentials([file(credentialsId: 'settingsFile', variable: 'FILE')]) {
	        sh "'${mvnHome}/bin/mvn' clean deploy -DskipTests -s $FILE"
	       }
	    }
	   }
	   stage('Deploy') {
	       if(params.deploy) {
	        sh "cp WLCPFrontEnd/target/*.war /home/wlcp/tomcat/webapps/WLCPFrontEnd.war"
	        sh "cp WLCPTestData/target/*.war /home/wlcp/tomcat/webapps/WLCPTestData.war"
	        sh "cp WLCPWebApp/target/*.war /home/wlcp/tomcat/webapps/WLCPWebApp.war"
	        sh "cp WLCPWebsite/target/*.war /home/wlcp/tomcat/webapps/WLCPWebsite.war"
	        sh "cp WLCPGameServer/target/*.jar /home/wlcp/tomcat/webapps/WLCPGameServer/WLCPGameServer.jar"
	       }
	   }
  } catch (e) {
  	currentBuild.result = "FAILED"
    throw e
  } finally {
  	notifyBuild(currentBuild.result)
  }
   
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  slackSend (color: colorCode, message: summary)
}
