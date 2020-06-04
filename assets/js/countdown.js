/** ********************************************** **
	@Author			Dorin Grigoras
	@Website		www.stepofweb.com
	@Last Update	3:12 AM Saturday, February 01, 2014
	********************************************** **

	USED BY:
		page-coming-soon-image.html
		page-coming-soon-video.html


	SET DATE
		TO set coundown date, edit the html file.
		SEARCH FOR LINES:

		<!-- Countdown -->
		<div id="countdown">
			<div class="countdown-widget" id="countdown-widget" data-time=""><!-- data-time example: 31 December 2015 12:00:00 GMT --></div>
		</div>
		<!-- /Countdown -->


	EXAMPLE:
		Change data-time="" 
			TO
	data-time="31 December 2015 12:00:00 GMT"
	
	That's all!
	It's highly recommended to not alter this file. Change the date only on html file(s).


	INFO
		Video is disabled on mobiles - image slider will take place.

		Please, also keep in mind: many other configuration are via html params data-*
		Be sure to inspect the html code first.

*************************************************** **/

	// init countdown
	var countdown_time 		= jQuery("#countdown-widget").data('time');
	var countdown_timezone 	= jQuery("#countdown-widget").data('timezone');
	
	if(countdown_time != '') {

		launchTime = new Date(Date.parse(countdown_time));

	} else {

		launchTime = new Date(); 						// Set launch: [year], [month], [day], [hour]...
		launchTime.setDate(launchTime.getDate() + 15); 	// Add 15 days

	}

	if(countdown_timezone == '')
		countdown_timezone = null;
			
	jQuery("#countdown-widget").countdown({
		until: launchTime, 
		format: "dHMS",
		labels: ['','','','','','',''],
		digits:['0','1','2','3','4','5','6','7','8','9'],
		timezone: countdown_timezone,

		labels: ['Years', 'Months', 'Weeks', 'Days', 'Hours', 'Minutes', 'Seconds'],
		labels1: ['Year', 'Month', 'Week', 'Day', 'Hour', 'Minute', 'Seconduy'],
	});


	// Video Background
	if(jQuery().mb_YTPlayer && jQuery("#countdown").length > 0) {

		var disableMobile = false;
		if( /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent) ) { 
			disableMobile = true; 
		}

		if(disableMobile === false) {

			jQuery(".player").mb_YTPlayer();

			jQuery("#video-volume").bind("click", function(e) {
				e.preventDefault();

				jQuery('#YTPlayer').toggleVolume();
			});

			// audio control
			jQuery("#video-volume").bind("click", function() {
				if(jQuery('i.fa', this).hasClass('fa-volume-down')) {
					jQuery('i.fa', this).removeClass('fa-volume-down');
					jQuery('i.fa', this).removeClass('fa-volume-up');
					jQuery('i.fa', this).addClass('fa-volume-up');
				} else {
					jQuery('i.fa', this).removeClass('fa-volume-up');
					jQuery('i.fa', this).removeClass('fa-volume-v');
					jQuery('i.fa', this).addClass('fa-volume-down');
				}
			});

			jQuery("#slider").fadeOut(500);
		} else {

			jQuery(".player , #video-volume").hide();
			jQuery("#slider").fadeIn(500);

		}

	}