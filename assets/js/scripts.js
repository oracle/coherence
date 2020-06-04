/** ********************************************** **
	@Author			Dorin Grigoras
	@Website		www.stepofweb.com
	@Last Update	Saturday, August 09, 2014

	NOTE! 	Do not change anything here if you want to
			be able to update in the future! Please use
			your custom script (eg. custom.js).


	TABLE CONTENTS
	-------------------------------
		01. Top Nav
		02. Animate
		03. OWL Carousel
		04. Popover
		05. LightBox
		06. ScrollTo
		07. Parallax
		08. Masonry Filter
		09. Toggle
		10. Background Image
		11. Quick Cart
		12. Placeholder
		13. Word Rotate
		14. Misc
		15. Datepicker
		16. Colorpicker
		17. Contact Google Map
		18. Newsletter Subscribe
		19. HTML Editor

	INLINE SCRIPTS
	-------------------------------

		COUNT TO
			https://github.com/mhuggins/jquery-countTo

		WAIT FOR IMAGES [used by masonry]
			https://github.com/alexanderdickson/waitForImages

		BROWSER DETECT

		SMOOTHSCROLL V1.2.1

		Datepicker v1.3.7
			https://github.com/dangrossman/bootstrap-daterangepicker

		Colorpicker v2.0
			http://mjolnic.github.io/bootstrap-colorpicker/

		Appear
			https://github.com/bas2k/jquery.appear/
			
		Parallax
			http://www.ianlunn.co.uk/plugins/jquery-parallax/

		jQuery Easing v1.3
			http://gsgd.co.uk/sandbox/jquery/easing/

		Backstretch v2.0.4
			http://srobbin.com/jquery-plugins/backstretch/
*************************************************** **/
 
	/* Init */
	jQuery(window).ready(function () {
		Epona();
	});

/** Core
 **************************************************************** **/
	function Epona() {
		jQuery.browserDetect();

		_topNav();
		_animate();
		_owl_carousel();
		_popover();
		_lightbox();
		_scrollTo();
		_parallax();
		_masonry();
		_toggle();
		_bgimage();
		_quickCart();
		_placeholder();
		_wrotate();
		_misc();
		_datepicker();
		_colorpicker();
		_newsletterSubscribe();
		_htmlEditor();

		/* --- */
		if(jQuery("html").hasClass("chrome") && jQuery("body").hasClass("smoothscroll")) {
			jQuery.smoothScroll();
		}
		/* --- */

		/** Bootstrap Tooltip **/ 
		jQuery("a[data-toggle=tooltip], span[data-toggle=tooltip]").tooltip();

		/** mobile - hide on click! **/
		jQuery(document).bind("click", function() {
			if(jQuery("div.navbar-collapse").hasClass('in')) {
				jQuery("button.btn-mobile").trigger('click');
			}
		});

		/** side nav **/
        jQuery("li.list-toggle").bind("click", function() {
            jQuery(this).toggleClass("active");
        });

		/** 
			Sticky Header 
			IE has strange bugs - we avoid sticky header on IE boxed version!
		**/
		if(jQuery('html').hasClass('ie') && jQuery('body').hasClass('boxed')) {} else {

			if(jQuery('#header.sticky').hasClass('has-slider')) {
				var _stickyMath = jQuery(".slider").outerHeight(true) + jQuery("#header").outerHeight(true);
			} else {
				var _stickyMath = jQuery("#header").outerHeight(true);
			}

			jQuery('#header.sticky').affix({

				offset: {
					top: _stickyMath
				},

				bottom: function () {
					return (this.bottom = jQuery('#footer').outerHeight(true))
				}

			});

		}

	}


/** 01. Top Nav
 **************************************************************** **/
function _topNav() {
	window.scrollTop 	= 0;

	jQuery(window).scroll(function() {
		_toTop();
	});

	/* Scroll To Top */
	function _toTop() {
		_scrollTop = jQuery(document).scrollTop();

		if(_scrollTop > 100) {

			if(jQuery("#toTop").is(":hidden")) {
				jQuery("#toTop").show();
			}

		} else {

			if(jQuery("#toTop").is(":visible")) {
				jQuery("#toTop").hide();
			}

		}

	}


	// Mobile Submenu
	var addActiveClass 	= false;
	jQuery("#topMain a.dropdown-toggle").bind("click", function(e) {
		if(jQuery(this).attr('href') == '#') {
			e.preventDefault();
		}

		addActiveClass = jQuery(this).parent().hasClass("resp-active");
		jQuery("#topMain").find(".resp-active").removeClass("resp-active");

		if(!addActiveClass) {
			jQuery(this).parents("li").addClass("resp-active");
		}

		return;

	});

	// Drop Downs - do not hide on click
	jQuery("#topMain li.dropdown, #topMain a.dropdown-toggle").bind("click", function(e) {
		e.stopPropagation();
	});

	// IE11 Bugfix
	// Version 1.1
	// Wednesday, July 23, 2014
	if(jQuery("html").hasClass("ie") || jQuery("html").hasClass("ff3")) {
		jQuery("#topNav ul.nav > li.mega-menu div").addClass('block');
		jQuery("#topNav ul.nav > li.mega-menu div div").addClass('pull-left');
	}
}



/** 02. Animate
 **************************************************************** **/
function _animate() {

	// Animation [appear]
	jQuery("[data-animation]").each(function() {
		var _t = jQuery(this);

		if(jQuery(window).width() > 767) {

			_t.appear(function() {

				var delay = (_t.attr("data-animation-delay") ? _t.attr("data-animation-delay") : 1);

				if(delay > 1) _t.css("animation-delay", delay + "ms");
				_t.addClass(_t.attr("data-animation"));

				setTimeout(function() {
					_t.addClass("animation-visible");
				}, delay);

			}, {accX: 0, accY: -50}); /* -150 */

		} else {

			_t.addClass("animation-visible");

		}

	});

	// Bootstrap Progress Bar
	jQuery("[data-appear-progress-animation]").each(function() {
		var $_t = jQuery(this);

		if(jQuery(window).width() > 767) {

			$_t.appear(function() {
				_delay = 1;

				if($_t.attr("data-appear-progress-animation-delay")) {
					_delay = $_t.attr("data-appear-progress-animation-delay");
				}

				if(_delay > 1) {
					$_t.css("animation-delay", _delay + "ms");
				}

				$_t.addClass($_t.attr("data-appear-progress-animation"));

				setTimeout(function() {

					$_t.addClass("animation-visible");

				}, _delay);

			}, {accX: 0, accY: -50}); /* -150 */

		} else {

			$_t.addClass("animation-visible");

		}

	});


	jQuery("[data-appear-progress-animation]").each(function() {
		var $_t = jQuery(this);

		$_t.appear(function() {

			var _delay = ($_t.attr("data-appear-animation-delay") ? $_t.attr("data-appear-animation-delay"): 1);

			if(_delay > 1) {
				$_t.css("animation-delay", _delay + "ms");
			}

			$_t.addClass($_t.attr("data-appear-animation"));
			setTimeout(function() {

				$_t.animate({"width": $_t.attr("data-appear-progress-animation")}, 1000, "easeOutQuad", function() {
					$_t.find(".progress-bar-tooltip").animate({"opacity": 1}, 500, "easeOutQuad");
				});

			}, _delay);

		}, {accX: 0, accY: -25});

	});


	// Count To
	jQuery(".countTo [data-to]").each(function() {
		var $_t = jQuery(this);

		$_t.appear(function() {

			$_t.countTo();

		}, {accX:0, accY: -50});

	});


	/* Knob Circular Bar */
	if(jQuery().knob) {

		jQuery(".knob").knob();

	}


	/* Animation */
	jQuery('.animate_from_top').each(function () {
		jQuery(this).appear(function() {
			jQuery(this).delay(150).animate({opacity:1,top:"0px"},1000);
		});	
	});

	jQuery('.animate_from_bottom').each(function () {
		jQuery(this).appear(function() {
			jQuery(this).delay(150).animate({opacity:1,bottom:"0px"},1000);
		});	
	});


	jQuery('.animate_from_left').each(function () {
		jQuery(this).appear(function() {
			jQuery(this).delay(150).animate({opacity:1,left:"0px"},1000);
		});	
	});


	jQuery('.animate_from_right').each(function () {
		jQuery(this).appear(function() {
			jQuery(this).delay(150).animate({opacity:1,right:"0px"},1000);
		});	
	});

	jQuery('.animate_fade_in').each(function () {
		jQuery(this).appear(function() {
			jQuery(this).delay(350).animate({opacity:1,right:"0px"},1000);
		});	
	});
}



/** 03. OWL Carousel
 **************************************************************** **/
function _owl_carousel() {

	var total = jQuery("div.owl-carousel").length,
		count = 0;

	jQuery("div.owl-carousel").each(function() {

		var slider 		= jQuery(this);
		var options 	= slider.attr('data-plugin-options');

		// Progress Bar
		var $opt = eval('(' + options + ')');  // convert text to json

		if($opt.progressBar == 'true') {
			var afterInit = progressBar;
		} else {
			var afterInit = false;
		}

		var defaults = {
			items: 					5,
			itemsCustom: 			false,
			itemsDesktop: 			[1199,4],
			itemsDesktopSmall: 		[980,3],
			itemsTablet: 			[768,2],
			itemsTabletSmall: 		false,
			itemsMobile: 			[479,1],
			singleItem: 			true,
			itemsScaleUp: 			false,

			slideSpeed: 			200,
			paginationSpeed: 		800,
			rewindSpeed: 			1000,

			autoPlay: 				false,
			stopOnHover: 			false,

			navigation: 			false,
			navigationText: [
								'<i class="fa fa-chevron-left"></i>',
								'<i class="fa fa-chevron-right"></i>'
							],
			rewindNav: 				true,
			scrollPerPage: 			false,

			pagination: 			true,
			paginationNumbers: 		false,

			responsive: 			true,
			responsiveRefreshRate: 	200,
			responsiveBaseWidth: 	window,

			baseClass: 				"owl-carousel",
			theme: 					"owl-theme",

			lazyLoad: 				false,
			lazyFollow: 			true,
			lazyEffect: 			"fade",

			autoHeight: 			false,

			jsonPath: 				false,
			jsonSuccess: 			false,

			dragBeforeAnimFinish: 	true,
			mouseDrag: 				true,
			touchDrag: 				true,

			transitionStyle: 		false,

			addClassActive: 		false,

			beforeUpdate: 			false,
			afterUpdate: 			false,
			beforeInit: 			false,
			afterInit: 				afterInit,
			beforeMove: 			false,
			afterMove: 				(afterInit == false) ? false : moved,
			afterAction: 			false,
			startDragging: 			false,
			afterLazyLoad: 			false
		}

		var config = jQuery.extend({}, defaults, options, slider.data("plugin-options"));
		slider.owlCarousel(config).addClass("owl-carousel-init");
		

		// Progress Bar
		var elem = jQuery(this);

		//Init progressBar where elem is $("#owl-demo")
		function progressBar(elem){
		  $elem = elem;
		  //build progress bar elements
		  buildProgressBar();
		  //start counting
		  start();
		}
	 
		//create div#progressBar and div#bar then prepend to $("#owl-demo")
		function buildProgressBar(){
		  $progressBar = $("<div>",{
			id:"progressBar"
		  });
		  $bar = $("<div>",{
			id:"bar"
		  });
		  $progressBar.append($bar).prependTo($elem);
		}

		function start() {
		  //reset timer
		  percentTime = 0;
		  isPause = false;
		  //run interval every 0.01 second
		  tick = setInterval(interval, 10);
		};

 
		var time = 7; // time in seconds
		function interval() {
		  if(isPause === false){
			percentTime += 1 / time;
			$bar.css({
			   width: percentTime+"%"
			 });
			//if percentTime is equal or greater than 100
			if(percentTime >= 100){
			  //slide to next item 
			  $elem.trigger('owl.next')
			}
		  }
		}
	 
		//pause while dragging 
		function pauseOnDragging(){
		  isPause = true;
		}
	 
		//moved callback
		function moved(){
		  //clear interval
		  clearTimeout(tick);
		  //start again
		  start();
		}

	});
}




/** 04. Popover
 **************************************************************** **/
function _popover() {

		jQuery("a[data-toggle=popover]").bind("click", function(e) {
			jQuery('.popover-title .close').remove();
			e.preventDefault();
		});

		var isVisible 	= false,
			clickedAway = false;


		jQuery("a[data-toggle=popover], button[data-toggle=popover]").popover({

				html: true,
				trigger: 'manual'

			}).click(function(e) {

				jQuery(this).popover('show');
				
				clickedAway = false;
				isVisible = true;
				e.preventDefault();

			});

			jQuery(document).click(function(e) {
				if(isVisible & clickedAway) {

					jQuery("a[data-toggle=popover], button[data-toggle=popover]").popover('hide');
					isVisible = clickedAway = false;

				} else {


					clickedAway = true;

				}

			});

		jQuery("a[data-toggle=popover], button[data-toggle=popover]").popover({

			html: true,
			trigger: 'manual'

		}).click(function(e) {

			$(this).popover('show');
			$('.popover-title').append('<button type="button" class="close">&times;</button>');
			$('.close').click(function(e){

				jQuery("a[data-toggle=popover], button[data-toggle=popover]").popover('hide');

			});

			e.preventDefault();
		});


	// jQuery("a[data-toggle=popover], button[data-toggle=popover]").popover();
}



/** 05. LightBox
 **************************************************************** **/
function _lightbox() {

	if(typeof(jQuery.magnificPopup) == "undefined") {
		return false;
	}

	jQuery.extend(true, jQuery.magnificPopup.defaults, {
		tClose: 		'Close',
		tLoading: 		'Loading...',

		gallery: {
			tPrev: 		'Previous',
			tNext: 		'Next',
			tCounter: 	'%curr% / %total%'
		},

		image: 	{ 
			tError: 	'Image not loaded!' 
		},

		ajax: 	{ 
			tError: 	'Content not loaded!' 
		}
	});

	jQuery(".lightbox").each(function() {

		var _t 			= jQuery(this),
			options 	= _t.attr('data-plugin-options'),
			config		= {},
			defaults 	= {
				type: 				'image',
				fixedContentPos: 	false,
				fixedBgPos: 		false,
				mainClass: 			'mfp-no-margins mfp-with-zoom',
				image: {
					verticalFit: 	true
				},

				zoom: {
					enabled: 		false,
					duration: 		300
				},

				gallery: {
					enabled: false,
					navigateByImgClick: true,
					preload: 			[0,1],
					arrowMarkup: 		'<button title="%title%" type="button" class="mfp-arrow mfp-arrow-%dir%"></button>',
					tPrev: 				'Previou',
					tNext: 				'Next',
					tCounter: 			'<span class="mfp-counter">%curr% / %total%</span>'
				},
			};

		if(_t.data("plugin-options")) {
			config = jQuery.extend({}, defaults, options, _t.data("plugin-options"));
		}

		jQuery(this).magnificPopup(config);

	});
}




/** 06. ScrollTo
 **************************************************************** **/
function _scrollTo() {

	jQuery("a.scrollTo").bind("click", function(e) {
		e.preventDefault();

		var href = jQuery(this).attr('href');

		if(href != '#') {
			jQuery('html,body').animate({scrollTop: jQuery(href).offset().top}, 800, 'easeInOutExpo');
		}
	});

	jQuery("a#toTop").bind("click", function(e) {
		e.preventDefault();
		jQuery('html,body').animate({scrollTop: 0}, 800, 'easeInOutExpo');
	});
}




/** 07. Parallax
 **************************************************************** **/
	function _parallax() {

		if(jQuery().parallax) {

			/* Default */
			jQuery(".parallax.parallax-default").css("background-attachment", "fixed");
			jQuery(".parallax.parallax-default").parallax("50%", "0.4");

			jQuery(".parallax.parallax-1").css("background-attachment", "fixed");
			jQuery(".parallax.parallax-1").parallax("50%", "0.4");

			jQuery(".parallax.parallax-2").css("background-attachment", "fixed");
			jQuery(".parallax.parallax-2").parallax("50%", "0.4");

			jQuery(".parallax.parallax-3").css("background-attachment", "fixed");
			jQuery(".parallax.parallax-3").parallax("50%", "0.4");

			jQuery(".parallax.parallax-4").css("background-attachment", "fixed");
			jQuery(".parallax.parallax-4").parallax("50%", "0.4");

			/* Home Slider */
			jQuery("#home div.slider").css("background-attachment", "fixed");
			jQuery("#home div.slider").parallax("50%", "0.4");

		}

	}




/** 08. Masonry Filter
 **************************************************************** **/
function _masonry() {

	jQuery(window).load(function() {

		jQuery("span.js_loader").remove();
		jQuery("li.masonry-item").addClass('fadeIn');

		jQuery(".masonry-list").each(function() {

			var _c = jQuery(this);

			_c.waitForImages(function() { // waitForImages Plugin - bottom of this file

				_c.masonry({
					itemSelector: '.masonry-item'
				});

			});

		});

	});

	jQuery("ul.isotope-filter").each(function() {

		var _el		 		= jQuery(this),
			destination 	= jQuery("ul.sort-destination[data-sort-id=" + jQuery(this).attr("data-sort-id") + "]");

		if(destination.get(0)) {

			jQuery(window).load(function() {

				destination.isotope({
					itemSelector: 	"li",
					layoutMode: 	'sloppyMasonry'
				});

				_el.find("a").click(function(e) {

					e.preventDefault();

					var $_t 	= jQuery(this),
						sortId 	= $_t.parents(".sort-source").attr("data-sort-id"),
						filter 	= $_t.parent().attr("data-option-value");

					_el.find("li.active").removeClass("active");
					$_t.parent().addClass("active");

					destination.isotope({
						filter: filter
					});

					jQuery(".sort-source-title[data-sort-id=" + sortId + "] strong").html($_t.html());
					return false;

				});

			});

		}

	});


	jQuery(window).load(function() {

		jQuery("ul.isotope").addClass('fadeIn');

	});
}




/** 09. Toggle
 **************************************************************** **/
function _toggle() {

	var $_t = this,
		previewParClosedHeight = 25;

	jQuery("div.toggle.active > p").addClass("preview-active");
	jQuery("div.toggle.active > div.toggle-content").slideDown(400);
	jQuery("div.toggle > label").click(function(e) {

		var parentSection 	= jQuery(this).parent(),
			parentWrapper 	= jQuery(this).parents("div.toggle"),
			previewPar 		= false,
			isAccordion 	= parentWrapper.hasClass("toggle-accordion");

		if(isAccordion && typeof(e.originalEvent) != "undefined") {
			parentWrapper.find("div.toggle.active > label").trigger("click");
		}

		parentSection.toggleClass("active");

		if(parentSection.find("> p").get(0)) {

			previewPar 					= parentSection.find("> p");
			var previewParCurrentHeight = previewPar.css("height");
			var previewParAnimateHeight = previewPar.css("height");
			previewPar.css("height", "auto");
			previewPar.css("height", previewParCurrentHeight);

		}

		var toggleContent = parentSection.find("> div.toggle-content");

		if(parentSection.hasClass("active")) {

			jQuery(previewPar).animate({height: previewParAnimateHeight}, 350, function() {jQuery(this).addClass("preview-active");});
			toggleContent.slideDown(350);

		} else {

			jQuery(previewPar).animate({height: previewParClosedHeight}, 350, function() {jQuery(this).removeClass("preview-active");});
			toggleContent.slideUp(350);

		}

	});
}



/** 10. Background Image
	class="boxed" should be added to body.
	Add to body - example: data-background="assets/images/boxed_background/1.jpg"
 **************************************************************** **/
	function _bgimage() {
		if(jQuery('body').hasClass('boxed')) {
			backgroundImageSwitch();
		}
		function backgroundImageSwitch() {
			var data_background = jQuery('body').attr('data-background');
			if(data_background) {
				jQuery.backstretch(data_background);
				jQuery('body').addClass('transparent'); // remove backround color of boxed class
			}
		}
	}



/** 11. Quick Cart
 **************************************************************** **/
	function _quickCart() {

		jQuery('li.quick-cart').bind("click", function() {

			jQuery('li.quick-cart .quick-cart-content').bind("click", function(e) {
				e.stopPropagation();
			});

			if(jQuery(this).hasClass('open')) {

				disable_overlay();
				enable_scroll();
				jQuery(this).removeClass('open');

			} else {

				enable_overlay();
				disable_scroll();
				jQuery(this).addClass('open');
				jQuery('li.search').removeClass('open'); // close search

			}

			return false;
		});

		// 'esc' key
		jQuery(document).keydown(function(e) {
			var code = e.keyCode ? e.keyCode : e.which;
			if(code == 27) {
				jQuery('li.search, li.quick-cart').removeClass('open');
				disable_overlay();
				enable_scroll();
			}
		});

		jQuery(document).bind("click", function() {
			if(jQuery('li.quick-cart').hasClass('open')) {
				jQuery('li.search, li.quick-cart').removeClass('open');
				disable_overlay();
				enable_scroll();
			}
		});

	}



/** 12. Placeholder
 **************************************************************** **/
	function _placeholder() {

		//check for IE
		if(navigator.appVersion.indexOf("MSIE")!=-1) {

			jQuery('[placeholder]').focus(function() {

				var input = jQuery(this);
				if (input.val() == input.attr('placeholder')) {
					input.val('');
					input.removeClass('placeholder');
				}

			}).blur(function() {

				var input = jQuery(this);
				if (input.val() == '' || input.val() == input.attr('placeholder')) {
				input.addClass('placeholder');
				input.val(input.attr('placeholder'));
				}

			}).blur();

		}

	}



/** 13. Word Rotate
 **************************************************************** **/
	function _wrotate() {
		jQuery(".word-rotator").each(function() {

			var _t 				= jQuery(this),
				_items 			= _t.find(".items"),
				items 			= _items.find("> span"),
				firstItem 		= items.eq(0),
				firstItemClone 	= firstItem.clone(),
				_iHeight 		= jQuery(this).height(),
				_cItem 			= 1,
				_cTop 			= 0,
				_delay 			= jQuery(this).attr('data-delay') || 2000;

			_items.append(firstItemClone);
			_t.height(_iHeight).addClass("active");

			setInterval(function() {
				_cTop = (_cItem * _iHeight);

				_items.animate({top: - (_cTop) + "px"}, 300, "easeOutQuad", function(){
					_cItem++;

					if(_cItem > items.length) {
						_items.css("top", 0);
						_cItem = 1;
					}

				});

			}, _delay);

		});
	}



/** 14. Misc
 **************************************************************** **/
	function _misc() {

		/*
			@FLEX SLIDER
		*/
		if(jQuery().flexslider && jQuery("div.flexslider").length > 0) {
			jQuery("div.flexslider").flexslider(flex_options);
		}

		/* 
			@LAYER SLIDER
		*/
		if(jQuery().layerSlider && jQuery("div.layerslider").length > 0) {
			jQuery("div.layerslider").each(function() {
				jQuery(this).layerSlider(layer_options);
			});
		}

	}



/** 15. Datepicker
 **************************************************************** **/
	function _datepicker() {
		jQuery('.demo-datepicker').daterangepicker({ singleDatePicker: true }, function(start, end, label) {});
		jQuery('.demo-rangepicker').daterangepicker(null, function(start, end, label) {});
	}



/** 16. Colorpicker
 **************************************************************** **/
	function _colorpicker() {
		jQuery('.colorpicker').colorpicker();
		jQuery('.colorpicker-element').colorpicker();
	}



/**	17. Contact Google Map
*************************************************** **/
	function contactMap() {

		var latLang = new google.maps.LatLng($googlemap_latitude,$googlemap_longitude);

		var mapOptions = {
			zoom:$googlemap_zoom,
			center: latLang,
			disableDefaultUI: false,
			navigationControl: false,
			mapTypeControl: false,
			scrollwheel: false,
			// styles: styles,
			mapTypeId: google.maps.MapTypeId.ROADMAP
		};

		var map = new google.maps.Map(document.getElementById('gmap'), mapOptions);
		google.maps.event.trigger(map, 'resize');
		map.setZoom( map.getZoom() );

		var marker = new google.maps.Marker({
			icon: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACcAAAArCAYAAAD7YZFOAAAACXBIWXMAAAsTAAALEwEAmpwYAAAKT2lDQ1BQaG90b3Nob3AgSUNDIHByb2ZpbGUAAHjanVNnVFPpFj333vRCS4iAlEtvUhUIIFJCi4AUkSYqIQkQSoghodkVUcERRUUEG8igiAOOjoCMFVEsDIoK2AfkIaKOg6OIisr74Xuja9a89+bN/rXXPues852zzwfACAyWSDNRNYAMqUIeEeCDx8TG4eQuQIEKJHAAEAizZCFz/SMBAPh+PDwrIsAHvgABeNMLCADATZvAMByH/w/qQplcAYCEAcB0kThLCIAUAEB6jkKmAEBGAYCdmCZTAKAEAGDLY2LjAFAtAGAnf+bTAICd+Jl7AQBblCEVAaCRACATZYhEAGg7AKzPVopFAFgwABRmS8Q5ANgtADBJV2ZIALC3AMDOEAuyAAgMADBRiIUpAAR7AGDIIyN4AISZABRG8lc88SuuEOcqAAB4mbI8uSQ5RYFbCC1xB1dXLh4ozkkXKxQ2YQJhmkAuwnmZGTKBNA/g88wAAKCRFRHgg/P9eM4Ors7ONo62Dl8t6r8G/yJiYuP+5c+rcEAAAOF0ftH+LC+zGoA7BoBt/qIl7gRoXgugdfeLZrIPQLUAoOnaV/Nw+H48PEWhkLnZ2eXk5NhKxEJbYcpXff5nwl/AV/1s+X48/Pf14L7iJIEyXYFHBPjgwsz0TKUcz5IJhGLc5o9H/LcL//wd0yLESWK5WCoU41EScY5EmozzMqUiiUKSKcUl0v9k4t8s+wM+3zUAsGo+AXuRLahdYwP2SycQWHTA4vcAAPK7b8HUKAgDgGiD4c93/+8//UegJQCAZkmScQAAXkQkLlTKsz/HCAAARKCBKrBBG/TBGCzABhzBBdzBC/xgNoRCJMTCQhBCCmSAHHJgKayCQiiGzbAdKmAv1EAdNMBRaIaTcA4uwlW4Dj1wD/phCJ7BKLyBCQRByAgTYSHaiAFiilgjjggXmYX4IcFIBBKLJCDJiBRRIkuRNUgxUopUIFVIHfI9cgI5h1xGupE7yAAygvyGvEcxlIGyUT3UDLVDuag3GoRGogvQZHQxmo8WoJvQcrQaPYw2oefQq2gP2o8+Q8cwwOgYBzPEbDAuxsNCsTgsCZNjy7EirAyrxhqwVqwDu4n1Y8+xdwQSgUXACTYEd0IgYR5BSFhMWE7YSKggHCQ0EdoJNwkDhFHCJyKTqEu0JroR+cQYYjIxh1hILCPWEo8TLxB7iEPENyQSiUMyJ7mQAkmxpFTSEtJG0m5SI+ksqZs0SBojk8naZGuyBzmULCAryIXkneTD5DPkG+Qh8lsKnWJAcaT4U+IoUspqShnlEOU05QZlmDJBVaOaUt2ooVQRNY9aQq2htlKvUYeoEzR1mjnNgxZJS6WtopXTGmgXaPdpr+h0uhHdlR5Ol9BX0svpR+iX6AP0dwwNhhWDx4hnKBmbGAcYZxl3GK+YTKYZ04sZx1QwNzHrmOeZD5lvVVgqtip8FZHKCpVKlSaVGyovVKmqpqreqgtV81XLVI+pXlN9rkZVM1PjqQnUlqtVqp1Q61MbU2epO6iHqmeob1Q/pH5Z/YkGWcNMw09DpFGgsV/jvMYgC2MZs3gsIWsNq4Z1gTXEJrHN2Xx2KruY/R27iz2qqaE5QzNKM1ezUvOUZj8H45hx+Jx0TgnnKKeX836K3hTvKeIpG6Y0TLkxZVxrqpaXllirSKtRq0frvTau7aedpr1Fu1n7gQ5Bx0onXCdHZ4/OBZ3nU9lT3acKpxZNPTr1ri6qa6UbobtEd79up+6Ynr5egJ5Mb6feeb3n+hx9L/1U/W36p/VHDFgGswwkBtsMzhg8xTVxbzwdL8fb8VFDXcNAQ6VhlWGX4YSRudE8o9VGjUYPjGnGXOMk423GbcajJgYmISZLTepN7ppSTbmmKaY7TDtMx83MzaLN1pk1mz0x1zLnm+eb15vft2BaeFostqi2uGVJsuRaplnutrxuhVo5WaVYVVpds0atna0l1rutu6cRp7lOk06rntZnw7Dxtsm2qbcZsOXYBtuutm22fWFnYhdnt8Wuw+6TvZN9un2N/T0HDYfZDqsdWh1+c7RyFDpWOt6azpzuP33F9JbpL2dYzxDP2DPjthPLKcRpnVOb00dnF2e5c4PziIuJS4LLLpc+Lpsbxt3IveRKdPVxXeF60vWdm7Obwu2o26/uNu5p7ofcn8w0nymeWTNz0MPIQ+BR5dE/C5+VMGvfrH5PQ0+BZ7XnIy9jL5FXrdewt6V3qvdh7xc+9j5yn+M+4zw33jLeWV/MN8C3yLfLT8Nvnl+F30N/I/9k/3r/0QCngCUBZwOJgUGBWwL7+Hp8Ib+OPzrbZfay2e1BjKC5QRVBj4KtguXBrSFoyOyQrSH355jOkc5pDoVQfujW0Adh5mGLw34MJ4WHhVeGP45wiFga0TGXNXfR3ENz30T6RJZE3ptnMU85ry1KNSo+qi5qPNo3ujS6P8YuZlnM1VidWElsSxw5LiquNm5svt/87fOH4p3iC+N7F5gvyF1weaHOwvSFpxapLhIsOpZATIhOOJTwQRAqqBaMJfITdyWOCnnCHcJnIi/RNtGI2ENcKh5O8kgqTXqS7JG8NXkkxTOlLOW5hCepkLxMDUzdmzqeFpp2IG0yPTq9MYOSkZBxQqohTZO2Z+pn5mZ2y6xlhbL+xW6Lty8elQfJa7OQrAVZLQq2QqboVFoo1yoHsmdlV2a/zYnKOZarnivN7cyzytuQN5zvn//tEsIS4ZK2pYZLVy0dWOa9rGo5sjxxedsK4xUFK4ZWBqw8uIq2Km3VT6vtV5eufr0mek1rgV7ByoLBtQFr6wtVCuWFfevc1+1dT1gvWd+1YfqGnRs+FYmKrhTbF5cVf9go3HjlG4dvyr+Z3JS0qavEuWTPZtJm6ebeLZ5bDpaql+aXDm4N2dq0Dd9WtO319kXbL5fNKNu7g7ZDuaO/PLi8ZafJzs07P1SkVPRU+lQ27tLdtWHX+G7R7ht7vPY07NXbW7z3/T7JvttVAVVN1WbVZftJ+7P3P66Jqun4lvttXa1ObXHtxwPSA/0HIw6217nU1R3SPVRSj9Yr60cOxx++/p3vdy0NNg1VjZzG4iNwRHnk6fcJ3/ceDTradox7rOEH0x92HWcdL2pCmvKaRptTmvtbYlu6T8w+0dbq3nr8R9sfD5w0PFl5SvNUyWna6YLTk2fyz4ydlZ19fi753GDborZ752PO32oPb++6EHTh0kX/i+c7vDvOXPK4dPKy2+UTV7hXmq86X23qdOo8/pPTT8e7nLuarrlca7nuer21e2b36RueN87d9L158Rb/1tWeOT3dvfN6b/fF9/XfFt1+cif9zsu72Xcn7q28T7xf9EDtQdlD3YfVP1v+3Njv3H9qwHeg89HcR/cGhYPP/pH1jw9DBY+Zj8uGDYbrnjg+OTniP3L96fynQ89kzyaeF/6i/suuFxYvfvjV69fO0ZjRoZfyl5O/bXyl/erA6xmv28bCxh6+yXgzMV70VvvtwXfcdx3vo98PT+R8IH8o/2j5sfVT0Kf7kxmTk/8EA5jz/GMzLdsAAAAgY0hSTQAAeiUAAICDAAD5/wAAgOkAAHUwAADqYAAAOpgAABdvkl/FRgAABONJREFUeNrEmMFvG0UUh7+13dI0Ng0pVEJIEJCQcgmEI1zo7pEDyh+A1JY7EhUnTglIvSG1cEGIQ3JBAg5VwglBWW9JSQWFkoCsxFjJOgpWtlXjNE6dOl57h8vbauV61/baEU8aRfaMZ7/83pvfzKymlCIqDMOYBM4Bk8DZNkMs4DowBxSj5jJNk15CC4MzDOMsMB0CFBYWcBFYHgRcIgTsMpDtEQwZ/ycwwwAi1QI1IlCTfc47DbwAXOhnklblBgHmx3lgdiBwkspBgQUB34/7Y00p5Rd/tovxy1L0e8ApYAoY6+J3LwLFXhdEKlAjnVbhhTZWcVEWQSfVp+PUX0J8LGpVzpmmqZumWYwAf018Liq9Y3Fq7lxE/7xpmt3+xxfC/E1iKg5clGoXe5wvavybceAmI9JZ7HE+K0K9sdhW0iZWYjqAFfL95CDhlmPC7Q3KJKPgxvifIwru1ZhzhhV+MQ7c/TBvkoNALzEWsfpjwYXV1kiMffFyRF9R07SE9ngQ1hIdCn/aMIzzYZ3ZbFaTllBKvRtltJ7n5YDjwBPSjsv2mRKRtHZ76/UOCs0ahjFmmuZMEEomTExMTIyOjo5+omnaO1GSViqVW0AaUIEG0AQa0pqA5/dpuq6PALtdpKwIzHuet9hsNveVUqeTyeTbyWTyLTmhhIZSasuyrNcD6mgCoAlQE6gDh9I8QPlHpjhH8q6j0Wh8s7i4+AFwTBRPtaTRA1ygCjzwAX0rWThKv2o2mwvAAfBQFEsBQ8BJaWlR/0n5PgloPtzcEbIVl5aWvhVFHggksihOAsOBlpbvE49M2DTN+8D8EcHN67ruF71fU0og0oE2HADTWneIT48ILjivJik90aKYD6YFVq1KBC68VhwX76QaUBTrSYlCzwBPi8n7qp0QNatATeAe21s/GiSZUuqzbDZ7TGrrNPA88BLwHPAUkJE+gH3ZSmuPfK71dYRhGPYgTiRKqUXLsqbk4aeAM8CzAumvyIZAbQHrQEnU8x678QfUm+0XznGcr4BXBGxUlEoHvM4H2wX+Be4ErCb8RU6/6tVqtX9u3rz5uSg0FNhPE/JwV1K4CeQBWz43gnCJkJR83I9qtm2vAuOB+jojBjssyj2UFOZlEe61goXCWZY1p5S6EQdsZ2en6DhOXWprRKDSUnuaKFQA/gY2JK1uK1jkSbher1+KsU256+vrm7IK0/LX97AG4AA5eU223i6VHeGUUmppaSnruu7VXuC2t7e3q9VqMuD4Q6JWRdS6Bfwhqaz4ZhvnDtGwbftDpVS1G7CDg4OHhUJhR6BOymHSBe7KNfMX4LbYRrUTWCc4VSqVnN3d3SvdwBUKhXuBlalJkeeBG3Kg/QvYlo3f6+v2pZTygNrKyspsrVbLR01SKpX2y+WyJ75ZE4u4BfwE/CyQ5bDCj6McUqxl27ZnPM87bDfg8PCwadv2gTz4jqTwR+B74FcB3dd1vdELWEc4Ua/qOM5vjuN83W7M2tranuu6O8CavIBcAK6JVdwFDnVd9+LYUqqbUzZwL5/Pf5nJZN7IZDIv+x2bm5uVcrmcl3q6LarZUm9uXKhu0+qrdwDYq6url+r1elVWZ21jY+Ma8B1wVdTKATtAvV+wbpXzr2+71Wr190Kh8MX4+Ph7uVxuAfhBfGtLjuCuruuKAcV/AwDnrxMM7gFGVQAAAABJRU5ErkJggg==',
			position: latLang,
			map: map,
			title: ""
		});

		marker.setMap(map);
		google.maps.event.addListener(marker, "click", function() {
			// Add optionally an action for when the marker is clicked
		});

		// kepp googlemap responsive - center on resize
		google.maps.event.addDomListener(window, 'resize', function() {
			map.setCenter(latLang);
		});

	}

	
	function showMap(initWhat) {
		var script 		= document.createElement('script');
		script.type 	= 'text/javascript';
		script.src 		= '//maps.google.com/maps/api/js?key=AIzaSyCqCn84CgZN6o1Xc3P4dM657HIxkX3jzPY&callback='+initWhat;
		document.body.appendChild(script);
	}

	
	// INIT CONTACT, ONLY IF #gmap EXISRS
	if(jQuery("#gmap").length > 0) {
		showMap('contactMap');
	}




/**	18. Newsletter Subscribe
*************************************************** **/
	function _newsletterSubscribe() {

		jQuery("form#newsletterSubscribe button").bind("click", function(e) {
				e.preventDefault();

				var email 			= jQuery("form#newsletterSubscribe input").val(),				// required
					_action			= jQuery("#newsletterSubscribe").attr('action'),	// form action URL
					_method			= jQuery("#newsletterSubscribe").attr('method');	// form method

				// SEND VIA AJAX
				$.ajax({
					url: 	_action,
					data: 	{ajax:"true", action:'newsletter_subscribe', email:email},
					type: 	_method,
					error: 	function(XMLHttpRequest, textStatus, errorThrown) {

						alert('Server Internal Error'); // usualy on headers 404

					},

					success: function(data) {
						data = data.trim(); // remove output spaces


						// PHP RETURN: Mandatory Fields
						if(data == '_required_') {
							alert('Please complete all fields!');
						} else

						// PHP RETURN: INVALID EMAIL
						if(data == '_invalid_email_') {
							alert('Invalid Email!');
						} else

						// VALID EMAIL
						if(data == '_ok_') {

							jQuery("#newsletter_email").val('');
							// modal
							alert('Thank You!');

						} else {

							// PHPMAILER ERROR
							alert(data); 

						}
					}
				});

		});

	}


/** 19. HTML Editor
 **************************************************************** **/
	function _htmlEditor() {

		/**
			SUMMERNOTE - textarea html editor

			<script type="text/javascript" src="assets/plugins/html_summernote/summernote.min.js"></script>
		**/
		if(jQuery('textarea.summernote').length > 0 && jQuery().summernote) {
			jQuery('textarea.summernote').each(function() {
				jQuery(this).summernote({
					height: jQuery(this).attr('data-height') || 200,
					toolbar: [
					/*	[groupname, 	[button list]]	*/
						['style', 		['style']],
						['fontsize', 	['fontsize']],
						['style', 		['bold', 'italic', 'underline','strikethrough', 'clear']],
						['color', 		['color']],
						['para', 		['ul', 'ol', 'paragraph']],
						['table', 		['table']],
						['media', 		['link', 'picture', 'video']],
						['misc', 		['codeview', 'fullscreen', 'help']]
					]
				});
			});
		}

		/**
			MARKDOWN - textarea html editor

			<script type="text/javascript" src="assets/plugins/html_markdown/bootstrap-markdown.min.js"></script>
		**/
		if(jQuery('textarea.markdown').length > 0 && jQuery().markdown) {
			jQuery('textarea.markdown').each(function() {

				jQuery(this).markdown({
					autofocus:		false,
					savable:		false,
					height:			jQuery(this).attr('data-height') 	|| 'inherit',
					language:		jQuery(this).attr('data-language') 	|| ''
				});

			});
		}

	}


/** **************************************************************************************************************** **/
/** **************************************************************************************************************** **/
/** **************************************************************************************************************** **/
/** **************************************************************************************************************** **/



	// scroll 
	function wheel(e) {
	  e.preventDefault();
	}

	function disable_scroll() {
	  if (window.addEventListener) {
		  window.addEventListener('DOMMouseScroll', wheel, false);
	  }
	  window.onmousewheel = document.onmousewheel = wheel;
	}

	function enable_scroll() {
		if (window.removeEventListener) {
			window.removeEventListener('DOMMouseScroll', wheel, false);
		}
		window.onmousewheel = document.onmousewheel = document.onkeydown = null;  
	}

	// overlay
	function enable_overlay() {
		jQuery("span.global-overlay").remove(); // remove first!
		jQuery('body').append('<span class="global-overlay"></span>');
	}
	function disable_overlay() {
		jQuery("span.global-overlay").remove();
	}



/** COUNT TO
	https://github.com/mhuggins/jquery-countTo
 **************************************************************** **/
 (function ($) {
	$.fn.countTo = function (options) {
		options = options || {};

		return jQuery(this).each(function () {
			// set options for current element
			var settings = jQuery.extend({}, $.fn.countTo.defaults, {
				from:            jQuery(this).data('from'),
				to:              jQuery(this).data('to'),
				speed:           jQuery(this).data('speed'),
				refreshInterval: jQuery(this).data('refresh-interval'),
				decimals:        jQuery(this).data('decimals')
			}, options);

			// how many times to update the value, and how much to increment the value on each update
			var loops = Math.ceil(settings.speed / settings.refreshInterval),
				increment = (settings.to - settings.from) / loops;

			// references & variables that will change with each update
			var self = this,
				$self = jQuery(this),
				loopCount = 0,
				value = settings.from,
				data = $self.data('countTo') || {};

			$self.data('countTo', data);

			// if an existing interval can be found, clear it first
			if (data.interval) {
				clearInterval(data.interval);
			}
			data.interval = setInterval(updateTimer, settings.refreshInterval);

			// __construct the element with the starting value
			render(value);

			function updateTimer() {
				value += increment;
				loopCount++;

				render(value);

				if (typeof(settings.onUpdate) == 'function') {
					settings.onUpdate.call(self, value);
				}

				if (loopCount >= loops) {
					// remove the interval
					$self.removeData('countTo');
					clearInterval(data.interval);
					value = settings.to;

					if (typeof(settings.onComplete) == 'function') {
						settings.onComplete.call(self, value);
					}
				}
			}

			function render(value) {
				var formattedValue = settings.formatter.call(self, value, settings);
				$self.html(formattedValue);
			}
		});
	};

	$.fn.countTo.defaults = {
		from: 0,               // the number the element should start at
		to: 0,                 // the number the element should end at
		speed: 1000,           // how long it should take to count between the target numbers
		refreshInterval: 100,  // how often the element should be updated
		decimals: 0,           // the number of decimal places to show
		formatter: formatter,  // handler for formatting the value before rendering
		onUpdate: null,        // callback method for every time the element is updated
		onComplete: null       // callback method for when the element finishes updating
	};

	function formatter(value, settings) {
		return value.toFixed(settings.decimals);
	}
}(jQuery));


/** WAIT FOR IMAGES [used by masonry]
	https://github.com/alexanderdickson/waitForImages
 **************************************************************** **/
;(function ($) {
    // Namespace all events.
    var eventNamespace = 'waitForImages';

    // CSS properties which contain references to images.
    $.waitForImages = {
        hasImageProperties: ['backgroundImage', 'listStyleImage', 'borderImage', 'borderCornerImage', 'cursor']
    };

    // Custom selector to find `img` elements that have a valid `src` attribute and have not already loaded.
    $.expr[':'].uncached = function (obj) {
        // Ensure we are dealing with an `img` element with a valid `src` attribute.
        if (!$(obj).is('img[src!=""]')) {
            return false;
        }

        // Firefox's `complete` property will always be `true` even if the image has not been downloaded.
        // Doing it this way works in Firefox.
        var img = new Image();
        img.src = obj.src;
        return !img.complete;
    };

    $.fn.waitForImages = function (finishedCallback, eachCallback, waitForAll) {

        var allImgsLength = 0;
        var allImgsLoaded = 0;

        // Handle options object.
        if ($.isPlainObject(arguments[0])) {
            waitForAll = arguments[0].waitForAll;
            eachCallback = arguments[0].each;
			// This must be last as arguments[0]
			// is aliased with finishedCallback.
            finishedCallback = arguments[0].finished;
        }

        // Handle missing callbacks.
        finishedCallback = finishedCallback || $.noop;
        eachCallback = eachCallback || $.noop;

        // Convert waitForAll to Boolean
        waitForAll = !! waitForAll;

        // Ensure callbacks are functions.
        if (!$.isFunction(finishedCallback) || !$.isFunction(eachCallback)) {
            throw new TypeError('An invalid callback was supplied.');
        }

        return this.each(function () {
            // Build a list of all imgs, dependent on what images will be considered.
            var obj = $(this);
            var allImgs = [];
            // CSS properties which may contain an image.
            var hasImgProperties = $.waitForImages.hasImageProperties || [];
            // To match `url()` references.
            // Spec: http://www.w3.org/TR/CSS2/syndata.html#value-def-uri
            var matchUrl = /url\(\s*(['"]?)(.*?)\1\s*\)/g;

            if (waitForAll) {

                // Get all elements (including the original), as any one of them could have a background image.
                obj.find('*').addBack().each(function () {
                    var element = $(this);

                    // If an `img` element, add it. But keep iterating in case it has a background image too.
                    if (element.is('img:uncached')) {
                        allImgs.push({
                            src: element.attr('src'),
                            element: element[0]
                        });
                    }

                    $.each(hasImgProperties, function (i, property) {
                        var propertyValue = element.css(property);
                        var match;

                        // If it doesn't contain this property, skip.
                        if (!propertyValue) {
                            return true;
                        }

                        // Get all url() of this element.
                        while (match = matchUrl.exec(propertyValue)) {
                            allImgs.push({
                                src: match[2],
                                element: element[0]
                            });
                        }
                    });
                });
            } else {
                // For images only, the task is simpler.
                obj.find('img:uncached')
                    .each(function () {
                    allImgs.push({
                        src: this.src,
                        element: this
                    });
                });
            }

            allImgsLength = allImgs.length;
            allImgsLoaded = 0;

            // If no images found, don't bother.
            if (allImgsLength === 0) {
                finishedCallback.call(obj[0]);
            }

            $.each(allImgs, function (i, img) {

                var image = new Image();

                // Handle the image loading and error with the same callback.
                $(image).on('load.' + eventNamespace + ' error.' + eventNamespace, function (event) {
                    allImgsLoaded++;

                    // If an error occurred with loading the image, set the third argument accordingly.
                    eachCallback.call(img.element, allImgsLoaded, allImgsLength, event.type == 'load');

                    if (allImgsLoaded == allImgsLength) {
                        finishedCallback.call(obj[0]);
                        return false;
                    }

                });

                image.src = img.src;
            });
        });
    };
}(jQuery));


/** BROWSER DETECT
	Add browser to html class
 **************************************************************** **/
(function($) {
	$.extend({

		browserDetect: function() {

			var u = navigator.userAgent,
				ua = u.toLowerCase(),
				is = function (t) {
					return ua.indexOf(t) > -1;
				},
				g = 'gecko',
				w = 'webkit',
				s = 'safari',
				o = 'opera',
				h = document.documentElement,
				b = [(!(/opera|webtv/i.test(ua)) && /msie\s(\d)/.test(ua)) ? ('ie ie' + parseFloat(navigator.appVersion.split("MSIE")[1])) : is('firefox/2') ? g + ' ff2' : is('firefox/3.5') ? g + ' ff3 ff3_5' : is('firefox/3') ? g + ' ff3' : is('gecko/') ? g : is('opera') ? o + (/version\/(\d+)/.test(ua) ? ' ' + o + RegExp.jQuery1 : (/opera(\s|\/)(\d+)/.test(ua) ? ' ' + o + RegExp.jQuery2 : '')) : is('konqueror') ? 'konqueror' : is('chrome') ? w + ' chrome' : is('iron') ? w + ' iron' : is('applewebkit/') ? w + ' ' + s + (/version\/(\d+)/.test(ua) ? ' ' + s + RegExp.jQuery1 : '') : is('mozilla/') ? g : '', is('j2me') ? 'mobile' : is('iphone') ? 'iphone' : is('ipod') ? 'ipod' : is('mac') ? 'mac' : is('darwin') ? 'mac' : is('webtv') ? 'webtv' : is('win') ? 'win' : is('freebsd') ? 'freebsd' : (is('x11') || is('linux')) ? 'linux' : '', 'js'];

			c = b.join(' ');
			h.className += ' ' + c;

			var isIE11 = !(window.ActiveXObject) && "ActiveXObject" in window;

			if(isIE11) {
				jQuery('html').removeClass('gecko').addClass('ie ie11');
				return;
			}

		}

	});
})(jQuery);

 
/** SMOOTHSCROLL V1.2.1
	Licensed under the terms of the MIT license.
 **************************************************************** **/
(function($) {
	$.extend({

		smoothScroll: function() {

			// Scroll Variables (tweakable)
			var defaultOptions = {

				// Scrolling Core
				frameRate        : 60, // [Hz]
				animationTime    : 700, // [px]
				stepSize         : 120, // [px]

				// Pulse (less tweakable)
				// ratio of "tail" to "acceleration"
				pulseAlgorithm   : true,
				pulseScale       : 10,
				pulseNormalize   : 1,

				// Acceleration
				accelerationDelta : 20,  // 20
				accelerationMax   : 1,   // 1

				// Keyboard Settings
				keyboardSupport   : true,  // option
				arrowScroll       : 50,     // [px]

				// Other
				touchpadSupport   : true,
				fixedBackground   : true,
				excluded          : ""
			};

			var options = defaultOptions;

			// Other Variables
			var isExcluded = false;
			var isFrame = false;
			var direction = { x: 0, y: 0 };
			var initDone  = false;
			var root = document.documentElement;
			var activeElement;
			var observer;
			var deltaBuffer = [ 120, 120, 120 ];

			var key = { left: 37, up: 38, right: 39, down: 40, spacebar: 32,
						pageup: 33, pagedown: 34, end: 35, home: 36 };


			/***********************************************
			 * INITIALIZE
			 ***********************************************/

			/**
			 * Tests if smooth scrolling is allowed. Shuts down everything if not.
			 */
			function initTest() {

				var disableKeyboard = false;

				// disable keys for google reader (spacebar conflict)
				if (document.URL.indexOf("google.com/reader/view") > -1) {
					disableKeyboard = true;
				}

				// disable everything if the page is blacklisted
				if (options.excluded) {
					var domains = options.excluded.split(/[,\n] ?/);
					domains.push("mail.google.com"); // exclude Gmail for now
					for (var i = domains.length; i--;) {
						if (document.URL.indexOf(domains[i]) > -1) {
							observer && observer.disconnect();
							removeEvent("mousewheel", wheel);
							disableKeyboard = true;
							isExcluded = true;
							break;
						}
					}
				}

				// disable keyboard support if anything above requested it
				if (disableKeyboard) {
					removeEvent("keydown", keydown);
				}

				if (options.keyboardSupport && !disableKeyboard) {
					addEvent("keydown", keydown);
				}
			}

			/**
			 * Sets up scrolls array, determines if frames are involved.
			 */
			function init() {

				if (!document.body) return;

				var body = document.body;
				var html = document.documentElement;
				var windowHeight = window.innerHeight;
				var scrollHeight = body.scrollHeight;

				// check compat mode for root element
				root = (document.compatMode.indexOf('CSS') >= 0) ? html : body;
				activeElement = body;

				initTest();
				initDone = true;

				// Checks if this script is running in a frame
				if (top != self) {
					isFrame = true;
				}

				/**
				 * This fixes a bug where the areas left and right to
				 * the content does not trigger the onmousewheel event
				 * on some pages. e.g.: html, body { height: 100% }
				 */
				else if (scrollHeight > windowHeight &&
						(body.offsetHeight <= windowHeight ||
						 html.offsetHeight <= windowHeight)) {

					// DOMChange (throttle): fix height
					var pending = false;
					var refresh = function () {
						if (!pending && html.scrollHeight != document.height) {
							pending = true; // add a new pending action
							setTimeout(function () {
								html.style.height = document.height + 'px';
								pending = false;
							}, 500); // act rarely to stay fast
						}
					};
					html.style.height = 'auto';
					setTimeout(refresh, 10);

					var config = {
						attributes: true,
						childList: true,
						characterData: false
					};

					observer = new MutationObserver(refresh);
					observer.observe(body, config);

					// clearfix
					if (root.offsetHeight <= windowHeight) {
						var underlay = document.createElement("div");
						underlay.style.clear = "both";
						body.appendChild(underlay);
					}
				}

				// gmail performance fix
				if (document.URL.indexOf("mail.google.com") > -1) {
					var s = document.createElement("style");
					s.innerHTML = ".iu { visibility: hidden }";
					(document.getElementsByTagName("head")[0] || html).appendChild(s);
				}
				// facebook better home timeline performance
				// all the HTML resized images make rendering CPU intensive
				else if (document.URL.indexOf("www.facebook.com") > -1) {
					var home_stream = document.getElementById("home_stream");
					home_stream && (home_stream.style.webkitTransform = "translateZ(0)");
				}
				// disable fixed background
				if (!options.fixedBackground && !isExcluded) {
					body.style.backgroundAttachment = "scroll";
					html.style.backgroundAttachment = "scroll";
				}
			}


			/************************************************
			 * SCROLLING
			 ************************************************/

			var que = [];
			var pending = false;
			var lastScroll = +new Date;

			/**
			 * Pushes scroll actions to the scrolling queue.
			 */
			function scrollArray(elem, left, top, delay) {

				delay || (delay = 1000);
				directionCheck(left, top);

				if (options.accelerationMax != 1) {
					var now = +new Date;
					var elapsed = now - lastScroll;
					if (elapsed < options.accelerationDelta) {
						var factor = (1 + (30 / elapsed)) / 2;
						if (factor > 1) {
							factor = Math.min(factor, options.accelerationMax);
							left *= factor;
							top  *= factor;
						}
					}
					lastScroll = +new Date;
				}

				// push a scroll command
				que.push({
					x: left,
					y: top,
					lastX: (left < 0) ? 0.99 : -0.99,
					lastY: (top  < 0) ? 0.99 : -0.99,
					start: +new Date
				});

				// don't act if there's a pending queue
				if (pending) {
					return;
				}

				var scrollWindow = (elem === document.body);

				var step = function (time) {

					var now = +new Date;
					var scrollX = 0;
					var scrollY = 0;

					for (var i = 0; i < que.length; i++) {

						var item = que[i];
						var elapsed  = now - item.start;
						var finished = (elapsed >= options.animationTime);

						// scroll position: [0, 1]
						var position = (finished) ? 1 : elapsed / options.animationTime;

						// easing [optional]
						if (options.pulseAlgorithm) {
							position = pulse(position);
						}

						// only need the difference
						var x = (item.x * position - item.lastX) >> 0;
						var y = (item.y * position - item.lastY) >> 0;

						// add this to the total scrolling
						scrollX += x;
						scrollY += y;

						// update last values
						item.lastX += x;
						item.lastY += y;

						// delete and step back if it's over
						if (finished) {
							que.splice(i, 1); i--;
						}
					}

					// scroll left and top
					if (scrollWindow) {
						window.scrollBy(scrollX, scrollY);
					}
					else {
						if (scrollX) elem.scrollLeft += scrollX;
						if (scrollY) elem.scrollTop  += scrollY;
					}

					// clean up if there's nothing left to do
					if (!left && !top) {
						que = [];
					}

					if (que.length) {
						requestFrame(step, elem, (delay / options.frameRate + 1));
					} else {
						pending = false;
					}
				};

				// start a new queue of actions
				requestFrame(step, elem, 0);
				pending = true;
			}


			/***********************************************
			 * EVENTS
			 ***********************************************/

			/**
			 * Mouse wheel handler.
			 * @param {Object} event
			 */
			function wheel(event) {

				if (!initDone) {
					init();
				}

				var target = event.target;
				var overflowing = overflowingAncestor(target);

				// use default if there's no overflowing
				// element or default action is prevented
				if (!overflowing || event.defaultPrevented ||
					isNodeName(activeElement, "embed") ||
				   (isNodeName(target, "embed") && /\.pdf/i.test(target.src))) {
					return true;
				}

				var deltaX = event.wheelDeltaX || 0;
				var deltaY = event.wheelDeltaY || 0;

				// use wheelDelta if deltaX/Y is not available
				if (!deltaX && !deltaY) {
					deltaY = event.wheelDelta || 0;
				}

				// check if it's a touchpad scroll that should be ignored
				if (!options.touchpadSupport && isTouchpad(deltaY)) {
					return true;
				}

				// scale by step size
				// delta is 120 most of the time
				// synaptics seems to send 1 sometimes
				if (Math.abs(deltaX) > 1.2) {
					deltaX *= options.stepSize / 120;
				}
				if (Math.abs(deltaY) > 1.2) {
					deltaY *= options.stepSize / 120;
				}

				scrollArray(overflowing, -deltaX, -deltaY);
				event.preventDefault();
			}

			/**
			 * Keydown event handler.
			 * @param {Object} event
			 */
			function keydown(event) {

				var target   = event.target;
				var modifier = event.ctrlKey || event.altKey || event.metaKey ||
							  (event.shiftKey && event.keyCode !== key.spacebar);

				// do nothing if user is editing text
				// or using a modifier key (except shift)
				// or in a dropdown
				if ( /input|textarea|select|embed/i.test(target.nodeName) ||
					 target.isContentEditable ||
					 event.defaultPrevented   ||
					 modifier ) {
				  return true;
				}
				// spacebar should trigger button press
				if (isNodeName(target, "button") &&
					event.keyCode === key.spacebar) {
				  return true;
				}

				var shift, x = 0, y = 0;
				var elem = overflowingAncestor(activeElement);
				var clientHeight = elem.clientHeight;

				if (elem == document.body) {
					clientHeight = window.innerHeight;
				}

				switch (event.keyCode) {
					case key.up:
						y = -options.arrowScroll;
						break;
					case key.down:
						y = options.arrowScroll;
						break;
					case key.spacebar: // (+ shift)
						shift = event.shiftKey ? 1 : -1;
						y = -shift * clientHeight * 0.9;
						break;
					case key.pageup:
						y = -clientHeight * 0.9;
						break;
					case key.pagedown:
						y = clientHeight * 0.9;
						break;
					case key.home:
						y = -elem.scrollTop;
						break;
					case key.end:
						var damt = elem.scrollHeight - elem.scrollTop - clientHeight;
						y = (damt > 0) ? damt+10 : 0;
						break;
					case key.left:
						x = -options.arrowScroll;
						break;
					case key.right:
						x = options.arrowScroll;
						break;
					default:
						return true; // a key we don't care about
				}

				scrollArray(elem, x, y);
				event.preventDefault();
			}

			/**
			 * Mousedown event only for updating activeElement
			 */
			function mousedown(event) {
				activeElement = event.target;
			}


			/***********************************************
			 * OVERFLOW
			 ***********************************************/

			var cache = {}; // cleared out every once in while
			setInterval(function () { cache = {}; }, 10 * 1000);

			var uniqueID = (function () {
				var i = 0;
				return function (el) {
					return el.uniqueID || (el.uniqueID = i++);
				};
			})();

			function setCache(elems, overflowing) {
				for (var i = elems.length; i--;)
					cache[uniqueID(elems[i])] = overflowing;
				return overflowing;
			}

			function overflowingAncestor(el) {
				var elems = [];
				var rootScrollHeight = root.scrollHeight;
				do {
					var cached = cache[uniqueID(el)];
					if (cached) {
						return setCache(elems, cached);
					}
					elems.push(el);
					if (rootScrollHeight === el.scrollHeight) {
						if (!isFrame || root.clientHeight + 10 < rootScrollHeight) {
							return setCache(elems, document.body); // scrolling root in WebKit
						}
					} else if (el.clientHeight + 10 < el.scrollHeight) {
						overflow = getComputedStyle(el, "").getPropertyValue("overflow-y");
						if (overflow === "scroll" || overflow === "auto") {
							return setCache(elems, el);
						}
					}
				} while (el = el.parentNode);
			}


			/***********************************************
			 * HELPERS
			 ***********************************************/

			function addEvent(type, fn, bubble) {
				window.addEventListener(type, fn, (bubble||false));
			}

			function removeEvent(type, fn, bubble) {
				window.removeEventListener(type, fn, (bubble||false));
			}

			function isNodeName(el, tag) {
				return (el.nodeName||"").toLowerCase() === tag.toLowerCase();
			}

			function directionCheck(x, y) {
				x = (x > 0) ? 1 : -1;
				y = (y > 0) ? 1 : -1;
				if (direction.x !== x || direction.y !== y) {
					direction.x = x;
					direction.y = y;
					que = [];
					lastScroll = 0;
				}
			}

			var deltaBufferTimer;

			function isTouchpad(deltaY) {
				if (!deltaY) return;
				deltaY = Math.abs(deltaY)
				deltaBuffer.push(deltaY);
				deltaBuffer.shift();
				clearTimeout(deltaBufferTimer);
				var allEquals    = (deltaBuffer[0] == deltaBuffer[1] &&
									deltaBuffer[1] == deltaBuffer[2]);
				var allDivisable = (isDivisible(deltaBuffer[0], 120) &&
									isDivisible(deltaBuffer[1], 120) &&
									isDivisible(deltaBuffer[2], 120));
				return !(allEquals || allDivisable);
			}

			function isDivisible(n, divisor) {
				return (Math.floor(n / divisor) == n / divisor);
			}

			var requestFrame = (function () {
				  return  window.requestAnimationFrame       ||
						  window.webkitRequestAnimationFrame ||
						  function (callback, element, delay) {
							  window.setTimeout(callback, delay || (1000/60));
						  };
			})();

			var MutationObserver = window.MutationObserver || window.WebKitMutationObserver;


			/***********************************************
			 * PULSE
			 ***********************************************/

			/**
			 * Viscous fluid with a pulse for part and decay for the rest.
			 * - Applies a fixed force over an interval (a damped acceleration), and
			 * - Lets the exponential bleed away the velocity over a longer interval
			 * - Michael Herf, http://stereopsis.com/stopping/
			 */
			function pulse_(x) {
				var val, start, expx;
				// test
				x = x * options.pulseScale;
				if (x < 1) { // acceleartion
					val = x - (1 - Math.exp(-x));
				} else {     // tail
					// the previous animation ended here:
					start = Math.exp(-1);
					// simple viscous drag
					x -= 1;
					expx = 1 - Math.exp(-x);
					val = start + (expx * (1 - start));
				}
				return val * options.pulseNormalize;
			}

			function pulse(x) {
				if (x >= 1) return 1;
				if (x <= 0) return 0;

				if (options.pulseNormalize == 1) {
					options.pulseNormalize /= pulse_(1);
				}
				return pulse_(x);
			}

			addEvent("mousedown", mousedown);
			addEvent("mousewheel", wheel);
			addEvent("load", init);

		}

	});
})(jQuery);

/**	Bootstrap Datepicker v1.3.7
	https://github.com/eternicode/bootstrap-datepicker/
 **************************************************************** **/
(function(j,f){var c=j(window);function n(){return new Date(Date.UTC.apply(Date,arguments))}function g(){var q=new Date();return n(q.getFullYear(),q.getMonth(),q.getDate())}function l(q){return function(){return this[q].apply(this,arguments)}}var e=(function(){var q={get:function(r){return this.slice(r)[0]},contains:function(u){var t=u&&u.valueOf();for(var s=0,r=this.length;s<r;s++){if(this[s].valueOf()===t){return s}}return -1},remove:function(r){this.splice(r,1)},replace:function(r){if(!r){return}if(!j.isArray(r)){r=[r]}this.clear();this.push.apply(this,r)},clear:function(){this.length=0},copy:function(){var r=new e();r.replace(this);return r}};return function(){var r=[];r.push.apply(r,arguments);j.extend(r,q);return r}})();var k=function(r,q){this.dates=new e();this.viewDate=g();this.focusDate=null;this._process_options(q);this.element=j(r);this.isInline=false;this.isInput=this.element.is("input");this.component=this.element.is(".date")?this.element.find(".add-on, .input-group-addon, .btn"):false;this.hasInput=this.component&&this.element.find("input").length;if(this.component&&this.component.length===0){this.component=false}this.picker=j(m.template);this._buildEvents();this._attachEvents();if(this.isInline){this.picker.addClass("datepicker-inline").appendTo(this.element)}else{this.picker.addClass("datepicker-dropdown dropdown-menu")}if(this.o.rtl){this.picker.addClass("datepicker-rtl")}this.viewMode=this.o.startView;if(this.o.calendarWeeks){this.picker.find("tfoot th.today").attr("colspan",function(s,t){return parseInt(t)+1})}this._allow_update=false;this.setStartDate(this._o.startDate);this.setEndDate(this._o.endDate);this.setDaysOfWeekDisabled(this.o.daysOfWeekDisabled);this.fillDow();this.fillMonths();this._allow_update=true;this.update();this.showMode();if(this.isInline){this.show()}};k.prototype={constructor:k,_process_options:function(q){this._o=j.extend({},this._o,q);var u=this.o=j.extend({},this._o);var t=u.language;if(!b[t]){t=t.split("-")[0];if(!b[t]){t=h.language}}u.language=t;switch(u.startView){case 2:case"decade":u.startView=2;break;case 1:case"year":u.startView=1;break;default:u.startView=0}switch(u.minViewMode){case 1:case"months":u.minViewMode=1;break;case 2:case"years":u.minViewMode=2;break;default:u.minViewMode=0}u.startView=Math.max(u.startView,u.minViewMode);if(u.multidate!==true){u.multidate=Number(u.multidate)||false;if(u.multidate!==false){u.multidate=Math.max(0,u.multidate)}else{u.multidate=1}}u.multidateSeparator=String(u.multidateSeparator);u.weekStart%=7;u.weekEnd=((u.weekStart+6)%7);var r=m.parseFormat(u.format);if(u.startDate!==-Infinity){if(!!u.startDate){if(u.startDate instanceof Date){u.startDate=this._local_to_utc(this._zero_time(u.startDate))}else{u.startDate=m.parseDate(u.startDate,r,u.language)}}else{u.startDate=-Infinity}}if(u.endDate!==Infinity){if(!!u.endDate){if(u.endDate instanceof Date){u.endDate=this._local_to_utc(this._zero_time(u.endDate))}else{u.endDate=m.parseDate(u.endDate,r,u.language)}}else{u.endDate=Infinity}}u.daysOfWeekDisabled=u.daysOfWeekDisabled||[];if(!j.isArray(u.daysOfWeekDisabled)){u.daysOfWeekDisabled=u.daysOfWeekDisabled.split(/[,\s]*/)}u.daysOfWeekDisabled=j.map(u.daysOfWeekDisabled,function(w){return parseInt(w,10)});var s=String(u.orientation).toLowerCase().split(/\s+/g),v=u.orientation.toLowerCase();s=j.grep(s,function(w){return(/^auto|left|right|top|bottom$/).test(w)});u.orientation={x:"auto",y:"auto"};if(!v||v==="auto"){}else{if(s.length===1){switch(s[0]){case"top":case"bottom":u.orientation.y=s[0];break;case"left":case"right":u.orientation.x=s[0];break}}else{v=j.grep(s,function(w){return(/^left|right$/).test(w)});u.orientation.x=v[0]||"auto";v=j.grep(s,function(w){return(/^top|bottom$/).test(w)});u.orientation.y=v[0]||"auto"}}},_events:[],_secondaryEvents:[],_applyEvents:function(q){for(var r=0,t,s,u;r<q.length;r++){t=q[r][0];if(q[r].length===2){s=f;u=q[r][1]}else{if(q[r].length===3){s=q[r][1];u=q[r][2]}}t.on(u,s)}},_unapplyEvents:function(q){for(var r=0,t,u,s;r<q.length;r++){t=q[r][0];if(q[r].length===2){s=f;u=q[r][1]}else{if(q[r].length===3){s=q[r][1];u=q[r][2]}}t.off(u,s)}},_buildEvents:function(){if(this.isInput){this._events=[[this.element,{focus:j.proxy(this.show,this),keyup:j.proxy(function(q){if(j.inArray(q.keyCode,[27,37,39,38,40,32,13,9])===-1){this.update()}},this),keydown:j.proxy(this.keydown,this)}]]}else{if(this.component&&this.hasInput){this._events=[[this.element.find("input"),{focus:j.proxy(this.show,this),keyup:j.proxy(function(q){if(j.inArray(q.keyCode,[27,37,39,38,40,32,13,9])===-1){this.update()}},this),keydown:j.proxy(this.keydown,this)}],[this.component,{click:j.proxy(this.show,this)}]]}else{if(this.element.is("div")){this.isInline=true}else{this._events=[[this.element,{click:j.proxy(this.show,this)}]]}}}this._events.push([this.element,"*",{blur:j.proxy(function(q){this._focused_from=q.target},this)}],[this.element,{blur:j.proxy(function(q){this._focused_from=q.target},this)}]);this._secondaryEvents=[[this.picker,{click:j.proxy(this.click,this)}],[j(window),{resize:j.proxy(this.place,this)}],[j(document),{"mousedown touchstart":j.proxy(function(q){if(!(this.element.is(q.target)||this.element.find(q.target).length||this.picker.is(q.target)||this.picker.find(q.target).length)){this.hide()}},this)}]]},_attachEvents:function(){this._detachEvents();this._applyEvents(this._events)},_detachEvents:function(){this._unapplyEvents(this._events)},_attachSecondaryEvents:function(){this._detachSecondaryEvents();this._applyEvents(this._secondaryEvents)},_detachSecondaryEvents:function(){this._unapplyEvents(this._secondaryEvents)},_trigger:function(s,t){var r=t||this.dates.get(-1),q=this._utc_to_local(r);this.element.trigger({type:s,date:q,dates:j.map(this.dates,this._utc_to_local),format:j.proxy(function(u,w){if(arguments.length===0){u=this.dates.length-1;w=this.o.format}else{if(typeof u==="string"){w=u;u=this.dates.length-1}}w=w||this.o.format;var v=this.dates.get(u);return m.formatDate(v,w,this.o.language)},this)})},show:function(){if(!this.isInline){this.picker.appendTo("body")}this.picker.show();this.place();this._attachSecondaryEvents();this._trigger("show")},hide:function(){if(this.isInline){return}if(!this.picker.is(":visible")){return}this.focusDate=null;this.picker.hide().detach();this._detachSecondaryEvents();this.viewMode=this.o.startView;this.showMode();if(this.o.forceParse&&(this.isInput&&this.element.val()||this.hasInput&&this.element.find("input").val())){this.setValue()}this._trigger("hide")},remove:function(){this.hide();this._detachEvents();this._detachSecondaryEvents();this.picker.remove();delete this.element.data().datepicker;if(!this.isInput){delete this.element.data().date}},_utc_to_local:function(q){return q&&new Date(q.getTime()+(q.getTimezoneOffset()*60000))},_local_to_utc:function(q){return q&&new Date(q.getTime()-(q.getTimezoneOffset()*60000))},_zero_time:function(q){return q&&new Date(q.getFullYear(),q.getMonth(),q.getDate())},_zero_utc_time:function(q){return q&&new Date(Date.UTC(q.getUTCFullYear(),q.getUTCMonth(),q.getUTCDate()))},getDates:function(){return j.map(this.dates,this._utc_to_local)},getUTCDates:function(){return j.map(this.dates,function(q){return new Date(q)})},getDate:function(){return this._utc_to_local(this.getUTCDate())},getUTCDate:function(){return new Date(this.dates.get(-1))},setDates:function(){var q=j.isArray(arguments[0])?arguments[0]:arguments;this.update.apply(this,q);this._trigger("changeDate");this.setValue()},setUTCDates:function(){var q=j.isArray(arguments[0])?arguments[0]:arguments;this.update.apply(this,j.map(q,this._utc_to_local));this._trigger("changeDate");this.setValue()},setDate:l("setDates"),setUTCDate:l("setUTCDates"),setValue:function(){var q=this.getFormattedDate();if(!this.isInput){if(this.component){this.element.find("input").val(q).change()}}else{this.element.val(q).change()}},getFormattedDate:function(q){if(q===f){q=this.o.format}var r=this.o.language;return j.map(this.dates,function(s){return m.formatDate(s,q,r)}).join(this.o.multidateSeparator)},setStartDate:function(q){this._process_options({startDate:q});this.update();this.updateNavArrows()},setEndDate:function(q){this._process_options({endDate:q});this.update();this.updateNavArrows()},setDaysOfWeekDisabled:function(q){this._process_options({daysOfWeekDisabled:q});this.update();this.updateNavArrows()},place:function(){if(this.isInline){return}var E=this.picker.outerWidth(),A=this.picker.outerHeight(),u=10,w=c.width(),r=c.height(),v=c.scrollTop();var C=parseInt(this.element.parents().filter(function(){return j(this).css("z-index")!=="auto"}).first().css("z-index"))+10;var z=this.component?this.component.parent().offset():this.element.offset();var D=this.component?this.component.outerHeight(true):this.element.outerHeight(false);var t=this.component?this.component.outerWidth(true):this.element.outerWidth(false);var y=z.left,B=z.top;this.picker.removeClass("datepicker-orient-top datepicker-orient-bottom datepicker-orient-right datepicker-orient-left");if(this.o.orientation.x!=="auto"){this.picker.addClass("datepicker-orient-"+this.o.orientation.x);if(this.o.orientation.x==="right"){y-=E-t}}else{this.picker.addClass("datepicker-orient-left");if(z.left<0){y-=z.left-u}else{if(z.left+E>w){y=w-E-u}}}var q=this.o.orientation.y,s,x;if(q==="auto"){s=-v+z.top-A;x=v+r-(z.top+D+A);if(Math.max(s,x)===x){q="top"}else{q="bottom"}}this.picker.addClass("datepicker-orient-"+q);if(q==="top"){B+=D}else{B-=A+parseInt(this.picker.css("padding-top"))}this.picker.css({top:B,left:y,zIndex:C})},_allow_update:true,update:function(){if(!this._allow_update){return}var r=this.dates.copy(),s=[],q=false;if(arguments.length){j.each(arguments,j.proxy(function(u,t){if(t instanceof Date){t=this._local_to_utc(t)}s.push(t)},this));q=true}else{s=this.isInput?this.element.val():this.element.data("date")||this.element.find("input").val();if(s&&this.o.multidate){s=s.split(this.o.multidateSeparator)}else{s=[s]}delete this.element.data().date}s=j.map(s,j.proxy(function(t){return m.parseDate(t,this.o.format,this.o.language)},this));s=j.grep(s,j.proxy(function(t){return(t<this.o.startDate||t>this.o.endDate||!t)},this),true);this.dates.replace(s);if(this.dates.length){this.viewDate=new Date(this.dates.get(-1))}else{if(this.viewDate<this.o.startDate){this.viewDate=new Date(this.o.startDate)}else{if(this.viewDate>this.o.endDate){this.viewDate=new Date(this.o.endDate)}}}if(q){this.setValue()}else{if(s.length){if(String(r)!==String(this.dates)){this._trigger("changeDate")}}}if(!this.dates.length&&r.length){this._trigger("clearDate")}this.fill()},fillDow:function(){var r=this.o.weekStart,s="<tr>";if(this.o.calendarWeeks){var q='<th class="cw">&nbsp;</th>';s+=q;this.picker.find(".datepicker-days thead tr:first-child").prepend(q)}while(r<this.o.weekStart+7){s+='<th class="dow">'+b[this.o.language].daysMin[(r++)%7]+"</th>"}s+="</tr>";this.picker.find(".datepicker-days thead").append(s)},fillMonths:function(){var r="",q=0;while(q<12){r+='<span class="month">'+b[this.o.language].monthsShort[q++]+"</span>"}this.picker.find(".datepicker-months td").html(r)},setRange:function(q){if(!q||!q.length){delete this.range}else{this.range=j.map(q,function(r){return r.valueOf()})}this.fill()},getClassNames:function(s){var q=[],t=this.viewDate.getUTCFullYear(),u=this.viewDate.getUTCMonth(),r=new Date();if(s.getUTCFullYear()<t||(s.getUTCFullYear()===t&&s.getUTCMonth()<u)){q.push("old")}else{if(s.getUTCFullYear()>t||(s.getUTCFullYear()===t&&s.getUTCMonth()>u)){q.push("new")}}if(this.focusDate&&s.valueOf()===this.focusDate.valueOf()){q.push("focused")}if(this.o.todayHighlight&&s.getUTCFullYear()===r.getFullYear()&&s.getUTCMonth()===r.getMonth()&&s.getUTCDate()===r.getDate()){q.push("today")}if(this.dates.contains(s)!==-1){q.push("active")}if(s.valueOf()<this.o.startDate||s.valueOf()>this.o.endDate||j.inArray(s.getUTCDay(),this.o.daysOfWeekDisabled)!==-1){q.push("disabled")}if(this.range){if(s>this.range[0]&&s<this.range[this.range.length-1]){q.push("range")}if(j.inArray(s.valueOf(),this.range)!==-1){q.push("selected")}}return q},fill:function(){var L=new Date(this.viewDate),A=L.getUTCFullYear(),M=L.getUTCMonth(),F=this.o.startDate!==-Infinity?this.o.startDate.getUTCFullYear():-Infinity,J=this.o.startDate!==-Infinity?this.o.startDate.getUTCMonth():-Infinity,x=this.o.endDate!==Infinity?this.o.endDate.getUTCFullYear():Infinity,G=this.o.endDate!==Infinity?this.o.endDate.getUTCMonth():Infinity,y=b[this.o.language].today||b.en.today||"",s=b[this.o.language].clear||b.en.clear||"",u;this.picker.find(".datepicker-days thead th.datepicker-switch").text(b[this.o.language].months[M]+" "+A);this.picker.find("tfoot th.today").text(y).toggle(this.o.todayBtn!==false);this.picker.find("tfoot th.clear").text(s).toggle(this.o.clearBtn!==false);this.updateNavArrows();this.fillMonths();var O=n(A,M-1,28),I=m.getDaysInMonth(O.getUTCFullYear(),O.getUTCMonth());O.setUTCDate(I);O.setUTCDate(I-(O.getUTCDay()-this.o.weekStart+7)%7);var q=new Date(O);q.setUTCDate(q.getUTCDate()+42);q=q.valueOf();var z=[];var D;while(O.valueOf()<q){if(O.getUTCDay()===this.o.weekStart){z.push("<tr>");if(this.o.calendarWeeks){var r=new Date(+O+(this.o.weekStart-O.getUTCDay()-7)%7*86400000),v=new Date(Number(r)+(7+4-r.getUTCDay())%7*86400000),t=new Date(Number(t=n(v.getUTCFullYear(),0,1))+(7+4-t.getUTCDay())%7*86400000),B=(v-t)/86400000/7+1;z.push('<td class="cw">'+B+"</td>")}}D=this.getClassNames(O);D.push("day");if(this.o.beforeShowDay!==j.noop){var C=this.o.beforeShowDay(this._utc_to_local(O));if(C===f){C={}}else{if(typeof(C)==="boolean"){C={enabled:C}}else{if(typeof(C)==="string"){C={classes:C}}}}if(C.enabled===false){D.push("disabled")}if(C.classes){D=D.concat(C.classes.split(/\s+/))}if(C.tooltip){u=C.tooltip}}D=j.unique(D);z.push('<td class="'+D.join(" ")+'"'+(u?' title="'+u+'"':"")+">"+O.getUTCDate()+"</td>");if(O.getUTCDay()===this.o.weekEnd){z.push("</tr>")}O.setUTCDate(O.getUTCDate()+1)}this.picker.find(".datepicker-days tbody").empty().append(z.join(""));var w=this.picker.find(".datepicker-months").find("th:eq(1)").text(A).end().find("span").removeClass("active");j.each(this.dates,function(P,Q){if(Q.getUTCFullYear()===A){w.eq(Q.getUTCMonth()).addClass("active")}});if(A<F||A>x){w.addClass("disabled")}if(A===F){w.slice(0,J).addClass("disabled")}if(A===x){w.slice(G+1).addClass("disabled")}z="";A=parseInt(A/10,10)*10;var N=this.picker.find(".datepicker-years").find("th:eq(1)").text(A+"-"+(A+9)).end().find("td");A-=1;var E=j.map(this.dates,function(P){return P.getUTCFullYear()}),K;for(var H=-1;H<11;H++){K=["year"];if(H===-1){K.push("old")}else{if(H===10){K.push("new")}}if(j.inArray(A,E)!==-1){K.push("active")}if(A<F||A>x){K.push("disabled")}z+='<span class="'+K.join(" ")+'">'+A+"</span>";A+=1}N.html(z)},updateNavArrows:function(){if(!this._allow_update){return}var s=new Date(this.viewDate),q=s.getUTCFullYear(),r=s.getUTCMonth();switch(this.viewMode){case 0:if(this.o.startDate!==-Infinity&&q<=this.o.startDate.getUTCFullYear()&&r<=this.o.startDate.getUTCMonth()){this.picker.find(".prev").css({visibility:"hidden"})}else{this.picker.find(".prev").css({visibility:"visible"})}if(this.o.endDate!==Infinity&&q>=this.o.endDate.getUTCFullYear()&&r>=this.o.endDate.getUTCMonth()){this.picker.find(".next").css({visibility:"hidden"})}else{this.picker.find(".next").css({visibility:"visible"})}break;case 1:case 2:if(this.o.startDate!==-Infinity&&q<=this.o.startDate.getUTCFullYear()){this.picker.find(".prev").css({visibility:"hidden"})}else{this.picker.find(".prev").css({visibility:"visible"})}if(this.o.endDate!==Infinity&&q>=this.o.endDate.getUTCFullYear()){this.picker.find(".next").css({visibility:"hidden"})}else{this.picker.find(".next").css({visibility:"visible"})}break}},click:function(u){u.preventDefault();var v=j(u.target).closest("span, td, th"),x,w,y;if(v.length===1){switch(v[0].nodeName.toLowerCase()){case"th":switch(v[0].className){case"datepicker-switch":this.showMode(1);break;case"prev":case"next":var q=m.modes[this.viewMode].navStep*(v[0].className==="prev"?-1:1);switch(this.viewMode){case 0:this.viewDate=this.moveMonth(this.viewDate,q);this._trigger("changeMonth",this.viewDate);break;case 1:case 2:this.viewDate=this.moveYear(this.viewDate,q);if(this.viewMode===1){this._trigger("changeYear",this.viewDate)}break}this.fill();break;case"today":var r=new Date();r=n(r.getFullYear(),r.getMonth(),r.getDate(),0,0,0);this.showMode(-2);var s=this.o.todayBtn==="linked"?null:"view";this._setDate(r,s);break;case"clear":var t;if(this.isInput){t=this.element}else{if(this.component){t=this.element.find("input")}}if(t){t.val("").change()}this.update();this._trigger("changeDate");if(this.o.autoclose){this.hide()}break}break;case"span":if(!v.is(".disabled")){this.viewDate.setUTCDate(1);if(v.is(".month")){y=1;w=v.parent().find("span").index(v);x=this.viewDate.getUTCFullYear();this.viewDate.setUTCMonth(w);this._trigger("changeMonth",this.viewDate);if(this.o.minViewMode===1){this._setDate(n(x,w,y))}}else{y=1;w=0;x=parseInt(v.text(),10)||0;this.viewDate.setUTCFullYear(x);this._trigger("changeYear",this.viewDate);if(this.o.minViewMode===2){this._setDate(n(x,w,y))}}this.showMode(-1);this.fill()}break;case"td":if(v.is(".day")&&!v.is(".disabled")){y=parseInt(v.text(),10)||1;x=this.viewDate.getUTCFullYear();w=this.viewDate.getUTCMonth();if(v.is(".old")){if(w===0){w=11;x-=1}else{w-=1}}else{if(v.is(".new")){if(w===11){w=0;x+=1}else{w+=1}}}this._setDate(n(x,w,y))}break}}if(this.picker.is(":visible")&&this._focused_from){j(this._focused_from).focus()}delete this._focused_from},_toggle_multidate:function(r){var q=this.dates.contains(r);if(!r){this.dates.clear()}else{if(q!==-1){this.dates.remove(q)}else{this.dates.push(r)}}if(typeof this.o.multidate==="number"){while(this.dates.length>this.o.multidate){this.dates.remove(0)}}},_setDate:function(q,s){if(!s||s==="date"){this._toggle_multidate(q&&new Date(q))}if(!s||s==="view"){this.viewDate=q&&new Date(q)}this.fill();this.setValue();this._trigger("changeDate");var r;if(this.isInput){r=this.element}else{if(this.component){r=this.element.find("input")}}if(r){r.change()}if(this.o.autoclose&&(!s||s==="date")){this.hide()}},moveMonth:function(q,r){if(!q){return f}if(!r){return q}var u=new Date(q.valueOf()),y=u.getUTCDate(),v=u.getUTCMonth(),t=Math.abs(r),x,w;r=r>0?1:-1;if(t===1){w=r===-1?function(){return u.getUTCMonth()===v}:function(){return u.getUTCMonth()!==x};x=v+r;u.setUTCMonth(x);if(x<0||x>11){x=(x+12)%12}}else{for(var s=0;s<t;s++){u=this.moveMonth(u,r)}x=u.getUTCMonth();u.setUTCDate(y);w=function(){return x!==u.getUTCMonth()}}while(w()){u.setUTCDate(--y);u.setUTCMonth(x)}return u},moveYear:function(r,q){return this.moveMonth(r,q*12)},dateWithinRange:function(q){return q>=this.o.startDate&&q<=this.o.endDate},keydown:function(w){if(this.picker.is(":not(:visible)")){if(w.keyCode===27){this.show()}return}var s=false,r,q,u,v=this.focusDate||this.viewDate;switch(w.keyCode){case 27:if(this.focusDate){this.focusDate=null;this.viewDate=this.dates.get(-1)||this.viewDate;this.fill()}else{this.hide()}w.preventDefault();break;case 37:case 39:if(!this.o.keyboardNavigation){break}r=w.keyCode===37?-1:1;if(w.ctrlKey){q=this.moveYear(this.dates.get(-1)||g(),r);u=this.moveYear(v,r);this._trigger("changeYear",this.viewDate)}else{if(w.shiftKey){q=this.moveMonth(this.dates.get(-1)||g(),r);u=this.moveMonth(v,r);this._trigger("changeMonth",this.viewDate)}else{q=new Date(this.dates.get(-1)||g());q.setUTCDate(q.getUTCDate()+r);u=new Date(v);u.setUTCDate(v.getUTCDate()+r)}}if(this.dateWithinRange(q)){this.focusDate=this.viewDate=u;this.setValue();this.fill();w.preventDefault()}break;case 38:case 40:if(!this.o.keyboardNavigation){break}r=w.keyCode===38?-1:1;if(w.ctrlKey){q=this.moveYear(this.dates.get(-1)||g(),r);u=this.moveYear(v,r);this._trigger("changeYear",this.viewDate)}else{if(w.shiftKey){q=this.moveMonth(this.dates.get(-1)||g(),r);u=this.moveMonth(v,r);this._trigger("changeMonth",this.viewDate)}else{q=new Date(this.dates.get(-1)||g());q.setUTCDate(q.getUTCDate()+r*7);u=new Date(v);u.setUTCDate(v.getUTCDate()+r*7)}}if(this.dateWithinRange(q)){this.focusDate=this.viewDate=u;this.setValue();this.fill();w.preventDefault()}break;case 32:break;case 13:v=this.focusDate||this.dates.get(-1)||this.viewDate;this._toggle_multidate(v);s=true;this.focusDate=null;this.viewDate=this.dates.get(-1)||this.viewDate;this.setValue();this.fill();if(this.picker.is(":visible")){w.preventDefault();if(this.o.autoclose){this.hide()}}break;case 9:this.focusDate=null;this.viewDate=this.dates.get(-1)||this.viewDate;this.fill();this.hide();break}if(s){if(this.dates.length){this._trigger("changeDate")}else{this._trigger("clearDate")}var t;if(this.isInput){t=this.element}else{if(this.component){t=this.element.find("input")}}if(t){t.change()}}},showMode:function(q){if(q){this.viewMode=Math.max(this.o.minViewMode,Math.min(2,this.viewMode+q))}this.picker.find(">div").hide().filter(".datepicker-"+m.modes[this.viewMode].clsName).css("display","block");this.updateNavArrows()}};var p=function(r,q){this.element=j(r);this.inputs=j.map(q.inputs,function(s){return s.jquery?s[0]:s});delete q.inputs;j(this.inputs).datepicker(q).bind("changeDate",j.proxy(this.dateUpdated,this));this.pickers=j.map(this.inputs,function(s){return j(s).data("datepicker")});this.updateDates()};p.prototype={updateDates:function(){this.dates=j.map(this.pickers,function(q){return q.getUTCDate()});this.updateRanges()},updateRanges:function(){var q=j.map(this.dates,function(r){return r.valueOf()});j.each(this.pickers,function(r,s){s.setRange(q)})},dateUpdated:function(t){if(this.updating){return}this.updating=true;var u=j(t.target).data("datepicker"),s=u.getUTCDate(),r=j.inArray(t.target,this.inputs),q=this.inputs.length;if(r===-1){return}j.each(this.pickers,function(v,w){if(!w.getUTCDate()){w.setUTCDate(s)}});if(s<this.dates[r]){while(r>=0&&s<this.dates[r]){this.pickers[r--].setUTCDate(s)}}else{if(s>this.dates[r]){while(r<q&&s>this.dates[r]){this.pickers[r++].setUTCDate(s)}}}this.updateDates();delete this.updating},remove:function(){j.map(this.pickers,function(q){q.remove()});delete this.element.data().datepicker}};function i(t,w){var v=j(t).data(),q={},u,s=new RegExp("^"+w.toLowerCase()+"([A-Z])");w=new RegExp("^"+w.toLowerCase());function x(z,y){return y.toLowerCase()}for(var r in v){if(w.test(r)){u=r.replace(s,x);q[u]=v[r]}}return q}function a(s){var q={};if(!b[s]){s=s.split("-")[0];if(!b[s]){return}}var r=b[s];j.each(o,function(u,t){if(t in r){q[t]=r[t]}});return q}var d=j.fn.datepicker;j.fn.datepicker=function(s){var q=Array.apply(null,arguments);q.shift();var r;this.each(function(){var A=j(this),y=A.data("datepicker"),u=typeof s==="object"&&s;if(!y){var w=i(this,"date"),t=j.extend({},h,w,u),v=a(t.language),x=j.extend({},h,v,w,u);if(A.is(".input-daterange")||x.inputs){var z={inputs:x.inputs||A.find("input").toArray()};A.data("datepicker",(y=new p(this,j.extend(x,z))))}else{A.data("datepicker",(y=new k(this,x)))}}if(typeof s==="string"&&typeof y[s]==="function"){r=y[s].apply(y,q);if(r!==f){return false}}});if(r!==f){return r}else{return this}};var h=j.fn.datepicker.defaults={autoclose:false,beforeShowDay:j.noop,calendarWeeks:false,clearBtn:false,daysOfWeekDisabled:[],endDate:Infinity,forceParse:true,format:"mm/dd/yyyy",keyboardNavigation:true,language:"en",minViewMode:0,multidate:false,multidateSeparator:",",orientation:"auto",rtl:false,startDate:-Infinity,startView:0,todayBtn:false,todayHighlight:false,weekStart:0};var o=j.fn.datepicker.locale_opts=["format","rtl","weekStart"];j.fn.datepicker.Constructor=k;var b=j.fn.datepicker.dates={en:{days:["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"],daysShort:["Sun","Mon","Tue","Wed","Thu","Fri","Sat","Sun"],daysMin:["Su","Mo","Tu","We","Th","Fr","Sa","Su"],months:["January","February","March","April","May","June","July","August","September","October","November","December"],monthsShort:["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],today:"Today",clear:"Clear"}};var m={modes:[{clsName:"days",navFnc:"Month",navStep:1},{clsName:"months",navFnc:"FullYear",navStep:1},{clsName:"years",navFnc:"FullYear",navStep:10}],isLeapYear:function(q){return(((q%4===0)&&(q%100!==0))||(q%400===0))},getDaysInMonth:function(q,r){return[31,(m.isLeapYear(q)?29:28),31,30,31,30,31,31,30,31,30,31][r]},validParts:/dd?|DD?|mm?|MM?|yy(?:yy)?/g,nonpunctuation:/[^ -\/:-@\[\u3400-\u9fff-`{-~\t\n\r]+/g,parseFormat:function(s){var q=s.replace(this.validParts,"\0").split("\0"),r=s.match(this.validParts);if(!q||!q.length||!r||r.length===0){throw new Error("Invalid date format.")}return{separators:q,parts:r}},parseDate:function(H,E,B){if(!H){return f}if(H instanceof Date){return H}if(typeof E==="string"){E=m.parseFormat(E)}var t=/([\-+]\d+)([dmwy])/,z=H.match(/([\-+]\d+)([dmwy])/g),A,y,D;if(/^[\-+]\d+[dmwy]([\s,]+[\-+]\d+[dmwy])*$/.test(H)){H=new Date();for(D=0;D<z.length;D++){A=t.exec(z[D]);y=parseInt(A[1]);switch(A[2]){case"d":H.setUTCDate(H.getUTCDate()+y);break;case"m":H=k.prototype.moveMonth.call(k.prototype,H,y);break;case"w":H.setUTCDate(H.getUTCDate()+y*7);break;case"y":H=k.prototype.moveYear.call(k.prototype,H,y);break}}return n(H.getUTCFullYear(),H.getUTCMonth(),H.getUTCDate(),0,0,0)}z=H&&H.match(this.nonpunctuation)||[];H=new Date();var u={},F=["yyyy","yy","M","MM","m","mm","d","dd"],x={yyyy:function(J,s){return J.setUTCFullYear(s)},yy:function(J,s){return J.setUTCFullYear(2000+s)},m:function(J,s){if(isNaN(J)){return J}s-=1;while(s<0){s+=12}s%=12;J.setUTCMonth(s);while(J.getUTCMonth()!==s){J.setUTCDate(J.getUTCDate()-1)}return J},d:function(J,s){return J.setUTCDate(s)}},I,r;x.M=x.MM=x.mm=x.m;x.dd=x.d;H=n(H.getFullYear(),H.getMonth(),H.getDate(),0,0,0);var q=E.parts.slice();if(z.length!==q.length){q=j(q).filter(function(s,J){return j.inArray(J,F)!==-1}).toArray()}function G(){var s=this.slice(0,z[D].length),J=z[D].slice(0,s.length);return s===J}if(z.length===q.length){var C;for(D=0,C=q.length;D<C;D++){I=parseInt(z[D],10);A=q[D];if(isNaN(I)){switch(A){case"MM":r=j(b[B].months).filter(G);I=j.inArray(r[0],b[B].months)+1;break;case"M":r=j(b[B].monthsShort).filter(G);I=j.inArray(r[0],b[B].monthsShort)+1;break}}u[A]=I}var v,w;for(D=0;D<F.length;D++){w=F[D];if(w in u&&!isNaN(u[w])){v=new Date(H);x[w](v,u[w]);if(!isNaN(v)){H=v}}}}return H},formatDate:function(q,u,w){if(!q){return""}if(typeof u==="string"){u=m.parseFormat(u)}var v={d:q.getUTCDate(),D:b[w].daysShort[q.getUTCDay()],DD:b[w].days[q.getUTCDay()],m:q.getUTCMonth()+1,M:b[w].monthsShort[q.getUTCMonth()],MM:b[w].months[q.getUTCMonth()],yy:q.getUTCFullYear().toString().substring(2),yyyy:q.getUTCFullYear()};v.dd=(v.d<10?"0":"")+v.d;v.mm=(v.m<10?"0":"")+v.m;q=[];var t=j.extend([],u.separators);for(var s=0,r=u.parts.length;s<=r;s++){if(t.length){q.push(t.shift())}q.push(v[u.parts[s]])}return q.join("")},headTemplate:'<thead><tr><th class="prev">&laquo;</th><th colspan="5" class="datepicker-switch"></th><th class="next">&raquo;</th></tr></thead>',contTemplate:'<tbody><tr><td colspan="7"></td></tr></tbody>',footTemplate:'<tfoot><tr><th colspan="7" class="today"></th></tr><tr><th colspan="7" class="clear"></th></tr></tfoot>'};m.template='<div class="datepicker"><div class="datepicker-days"><table class=" table-condensed">'+m.headTemplate+"<tbody></tbody>"+m.footTemplate+'</table></div><div class="datepicker-months"><table class="table-condensed">'+m.headTemplate+m.contTemplate+m.footTemplate+'</table></div><div class="datepicker-years"><table class="table-condensed">'+m.headTemplate+m.contTemplate+m.footTemplate+"</table></div></div>";j.fn.datepicker.DPGlobal=m;j.fn.datepicker.noConflict=function(){j.fn.datepicker=d;return this};j(document).on("focus.datepicker.data-api click.datepicker.data-api",'[data-provide="datepicker"]',function(r){var q=j(this);if(q.data("datepicker")){return}r.preventDefault();q.datepicker("show")});j(function(){j('[data-provide="datepicker-inline"]').datepicker()})}(window.jQuery));

/**	Daterangepicker 1.3.8
	http://www.dangrossman.info/
 **************************************************************** **/
/* moment.js */(function(a){function b(){return{empty:!1,unusedTokens:[],unusedInput:[],overflow:-2,charsLeftOver:0,nullInput:!1,invalidMonth:null,invalidFormat:!1,userInvalidated:!1,iso:!1}}function c(a,b){return function(c){return k(a.call(this,c),b)}}function d(a,b){return function(c){return this.lang().ordinal(a.call(this,c),b)}}function e(){}function f(a){w(a),h(this,a)}function g(a){var b=q(a),c=b.year||0,d=b.month||0,e=b.week||0,f=b.day||0,g=b.hour||0,h=b.minute||0,i=b.second||0,j=b.millisecond||0;this._milliseconds=+j+1e3*i+6e4*h+36e5*g,this._days=+f+7*e,this._months=+d+12*c,this._data={},this._bubble()}function h(a,b){for(var c in b)b.hasOwnProperty(c)&&(a[c]=b[c]);return b.hasOwnProperty("toString")&&(a.toString=b.toString),b.hasOwnProperty("valueOf")&&(a.valueOf=b.valueOf),a}function i(a){var b,c={};for(b in a)a.hasOwnProperty(b)&&qb.hasOwnProperty(b)&&(c[b]=a[b]);return c}function j(a){return 0>a?Math.ceil(a):Math.floor(a)}function k(a,b,c){for(var d=""+Math.abs(a),e=a>=0;d.length<b;)d="0"+d;return(e?c?"+":"":"-")+d}function l(a,b,c,d){var e,f,g=b._milliseconds,h=b._days,i=b._months;g&&a._d.setTime(+a._d+g*c),(h||i)&&(e=a.minute(),f=a.hour()),h&&a.date(a.date()+h*c),i&&a.month(a.month()+i*c),g&&!d&&db.updateOffset(a),(h||i)&&(a.minute(e),a.hour(f))}function m(a){return"[object Array]"===Object.prototype.toString.call(a)}function n(a){return"[object Date]"===Object.prototype.toString.call(a)||a instanceof Date}function o(a,b,c){var d,e=Math.min(a.length,b.length),f=Math.abs(a.length-b.length),g=0;for(d=0;e>d;d++)(c&&a[d]!==b[d]||!c&&s(a[d])!==s(b[d]))&&g++;return g+f}function p(a){if(a){var b=a.toLowerCase().replace(/(.)s$/,"$1");a=Tb[a]||Ub[b]||b}return a}function q(a){var b,c,d={};for(c in a)a.hasOwnProperty(c)&&(b=p(c),b&&(d[b]=a[c]));return d}function r(b){var c,d;if(0===b.indexOf("week"))c=7,d="day";else{if(0!==b.indexOf("month"))return;c=12,d="month"}db[b]=function(e,f){var g,h,i=db.fn._lang[b],j=[];if("number"==typeof e&&(f=e,e=a),h=function(a){var b=db().utc().set(d,a);return i.call(db.fn._lang,b,e||"")},null!=f)return h(f);for(g=0;c>g;g++)j.push(h(g));return j}}function s(a){var b=+a,c=0;return 0!==b&&isFinite(b)&&(c=b>=0?Math.floor(b):Math.ceil(b)),c}function t(a,b){return new Date(Date.UTC(a,b+1,0)).getUTCDate()}function u(a){return v(a)?366:365}function v(a){return a%4===0&&a%100!==0||a%400===0}function w(a){var b;a._a&&-2===a._pf.overflow&&(b=a._a[jb]<0||a._a[jb]>11?jb:a._a[kb]<1||a._a[kb]>t(a._a[ib],a._a[jb])?kb:a._a[lb]<0||a._a[lb]>23?lb:a._a[mb]<0||a._a[mb]>59?mb:a._a[nb]<0||a._a[nb]>59?nb:a._a[ob]<0||a._a[ob]>999?ob:-1,a._pf._overflowDayOfYear&&(ib>b||b>kb)&&(b=kb),a._pf.overflow=b)}function x(a){return null==a._isValid&&(a._isValid=!isNaN(a._d.getTime())&&a._pf.overflow<0&&!a._pf.empty&&!a._pf.invalidMonth&&!a._pf.nullInput&&!a._pf.invalidFormat&&!a._pf.userInvalidated,a._strict&&(a._isValid=a._isValid&&0===a._pf.charsLeftOver&&0===a._pf.unusedTokens.length)),a._isValid}function y(a){return a?a.toLowerCase().replace("_","-"):a}function z(a,b){return b._isUTC?db(a).zone(b._offset||0):db(a).local()}function A(a,b){return b.abbr=a,pb[a]||(pb[a]=new e),pb[a].set(b),pb[a]}function B(a){delete pb[a]}function C(a){var b,c,d,e,f=0,g=function(a){if(!pb[a]&&rb)try{require("./lang/"+a)}catch(b){}return pb[a]};if(!a)return db.fn._lang;if(!m(a)){if(c=g(a))return c;a=[a]}for(;f<a.length;){for(e=y(a[f]).split("-"),b=e.length,d=y(a[f+1]),d=d?d.split("-"):null;b>0;){if(c=g(e.slice(0,b).join("-")))return c;if(d&&d.length>=b&&o(e,d,!0)>=b-1)break;b--}f++}return db.fn._lang}function D(a){return a.match(/\[[\s\S]/)?a.replace(/^\[|\]$/g,""):a.replace(/\\/g,"")}function E(a){var b,c,d=a.match(vb);for(b=0,c=d.length;c>b;b++)d[b]=Yb[d[b]]?Yb[d[b]]:D(d[b]);return function(e){var f="";for(b=0;c>b;b++)f+=d[b]instanceof Function?d[b].call(e,a):d[b];return f}}function F(a,b){return a.isValid()?(b=G(b,a.lang()),Vb[b]||(Vb[b]=E(b)),Vb[b](a)):a.lang().invalidDate()}function G(a,b){function c(a){return b.longDateFormat(a)||a}var d=5;for(wb.lastIndex=0;d>=0&&wb.test(a);)a=a.replace(wb,c),wb.lastIndex=0,d-=1;return a}function H(a,b){var c,d=b._strict;switch(a){case"DDDD":return Ib;case"YYYY":case"GGGG":case"gggg":return d?Jb:zb;case"Y":case"G":case"g":return Lb;case"YYYYYY":case"YYYYY":case"GGGGG":case"ggggg":return d?Kb:Ab;case"S":if(d)return Gb;case"SS":if(d)return Hb;case"SSS":if(d)return Ib;case"DDD":return yb;case"MMM":case"MMMM":case"dd":case"ddd":case"dddd":return Cb;case"a":case"A":return C(b._l)._meridiemParse;case"X":return Fb;case"Z":case"ZZ":return Db;case"T":return Eb;case"SSSS":return Bb;case"MM":case"DD":case"YY":case"GG":case"gg":case"HH":case"hh":case"mm":case"ss":case"ww":case"WW":return d?Hb:xb;case"M":case"D":case"d":case"H":case"h":case"m":case"s":case"w":case"W":case"e":case"E":return xb;default:return c=new RegExp(P(O(a.replace("\\","")),"i"))}}function I(a){a=a||"";var b=a.match(Db)||[],c=b[b.length-1]||[],d=(c+"").match(Qb)||["-",0,0],e=+(60*d[1])+s(d[2]);return"+"===d[0]?-e:e}function J(a,b,c){var d,e=c._a;switch(a){case"M":case"MM":null!=b&&(e[jb]=s(b)-1);break;case"MMM":case"MMMM":d=C(c._l).monthsParse(b),null!=d?e[jb]=d:c._pf.invalidMonth=b;break;case"D":case"DD":null!=b&&(e[kb]=s(b));break;case"DDD":case"DDDD":null!=b&&(c._dayOfYear=s(b));break;case"YY":e[ib]=s(b)+(s(b)>68?1900:2e3);break;case"YYYY":case"YYYYY":case"YYYYYY":e[ib]=s(b);break;case"a":case"A":c._isPm=C(c._l).isPM(b);break;case"H":case"HH":case"h":case"hh":e[lb]=s(b);break;case"m":case"mm":e[mb]=s(b);break;case"s":case"ss":e[nb]=s(b);break;case"S":case"SS":case"SSS":case"SSSS":e[ob]=s(1e3*("0."+b));break;case"X":c._d=new Date(1e3*parseFloat(b));break;case"Z":case"ZZ":c._useUTC=!0,c._tzm=I(b);break;case"w":case"ww":case"W":case"WW":case"d":case"dd":case"ddd":case"dddd":case"e":case"E":a=a.substr(0,1);case"gg":case"gggg":case"GG":case"GGGG":case"GGGGG":a=a.substr(0,2),b&&(c._w=c._w||{},c._w[a]=b)}}function K(a){var b,c,d,e,f,g,h,i,j,k,l=[];if(!a._d){for(d=M(a),a._w&&null==a._a[kb]&&null==a._a[jb]&&(f=function(b){var c=parseInt(b,10);return b?b.length<3?c>68?1900+c:2e3+c:c:null==a._a[ib]?db().weekYear():a._a[ib]},g=a._w,null!=g.GG||null!=g.W||null!=g.E?h=Z(f(g.GG),g.W||1,g.E,4,1):(i=C(a._l),j=null!=g.d?V(g.d,i):null!=g.e?parseInt(g.e,10)+i._week.dow:0,k=parseInt(g.w,10)||1,null!=g.d&&j<i._week.dow&&k++,h=Z(f(g.gg),k,j,i._week.doy,i._week.dow)),a._a[ib]=h.year,a._dayOfYear=h.dayOfYear),a._dayOfYear&&(e=null==a._a[ib]?d[ib]:a._a[ib],a._dayOfYear>u(e)&&(a._pf._overflowDayOfYear=!0),c=U(e,0,a._dayOfYear),a._a[jb]=c.getUTCMonth(),a._a[kb]=c.getUTCDate()),b=0;3>b&&null==a._a[b];++b)a._a[b]=l[b]=d[b];for(;7>b;b++)a._a[b]=l[b]=null==a._a[b]?2===b?1:0:a._a[b];l[lb]+=s((a._tzm||0)/60),l[mb]+=s((a._tzm||0)%60),a._d=(a._useUTC?U:T).apply(null,l)}}function L(a){var b;a._d||(b=q(a._i),a._a=[b.year,b.month,b.day,b.hour,b.minute,b.second,b.millisecond],K(a))}function M(a){var b=new Date;return a._useUTC?[b.getUTCFullYear(),b.getUTCMonth(),b.getUTCDate()]:[b.getFullYear(),b.getMonth(),b.getDate()]}function N(a){a._a=[],a._pf.empty=!0;var b,c,d,e,f,g=C(a._l),h=""+a._i,i=h.length,j=0;for(d=G(a._f,g).match(vb)||[],b=0;b<d.length;b++)e=d[b],c=(h.match(H(e,a))||[])[0],c&&(f=h.substr(0,h.indexOf(c)),f.length>0&&a._pf.unusedInput.push(f),h=h.slice(h.indexOf(c)+c.length),j+=c.length),Yb[e]?(c?a._pf.empty=!1:a._pf.unusedTokens.push(e),J(e,c,a)):a._strict&&!c&&a._pf.unusedTokens.push(e);a._pf.charsLeftOver=i-j,h.length>0&&a._pf.unusedInput.push(h),a._isPm&&a._a[lb]<12&&(a._a[lb]+=12),a._isPm===!1&&12===a._a[lb]&&(a._a[lb]=0),K(a),w(a)}function O(a){return a.replace(/\\(\[)|\\(\])|\[([^\]\[]*)\]|\\(.)/g,function(a,b,c,d,e){return b||c||d||e})}function P(a){return a.replace(/[-\/\\^$*+?.()|[\]{}]/g,"\\$&")}function Q(a){var c,d,e,f,g;if(0===a._f.length)return a._pf.invalidFormat=!0,a._d=new Date(0/0),void 0;for(f=0;f<a._f.length;f++)g=0,c=h({},a),c._pf=b(),c._f=a._f[f],N(c),x(c)&&(g+=c._pf.charsLeftOver,g+=10*c._pf.unusedTokens.length,c._pf.score=g,(null==e||e>g)&&(e=g,d=c));h(a,d||c)}function R(a){var b,c,d=a._i,e=Mb.exec(d);if(e){for(a._pf.iso=!0,b=0,c=Ob.length;c>b;b++)if(Ob[b][1].exec(d)){a._f=Ob[b][0]+(e[6]||" ");break}for(b=0,c=Pb.length;c>b;b++)if(Pb[b][1].exec(d)){a._f+=Pb[b][0];break}d.match(Db)&&(a._f+="Z"),N(a)}else a._d=new Date(d)}function S(b){var c=b._i,d=sb.exec(c);c===a?b._d=new Date:d?b._d=new Date(+d[1]):"string"==typeof c?R(b):m(c)?(b._a=c.slice(0),K(b)):n(c)?b._d=new Date(+c):"object"==typeof c?L(b):b._d=new Date(c)}function T(a,b,c,d,e,f,g){var h=new Date(a,b,c,d,e,f,g);return 1970>a&&h.setFullYear(a),h}function U(a){var b=new Date(Date.UTC.apply(null,arguments));return 1970>a&&b.setUTCFullYear(a),b}function V(a,b){if("string"==typeof a)if(isNaN(a)){if(a=b.weekdaysParse(a),"number"!=typeof a)return null}else a=parseInt(a,10);return a}function W(a,b,c,d,e){return e.relativeTime(b||1,!!c,a,d)}function X(a,b,c){var d=hb(Math.abs(a)/1e3),e=hb(d/60),f=hb(e/60),g=hb(f/24),h=hb(g/365),i=45>d&&["s",d]||1===e&&["m"]||45>e&&["mm",e]||1===f&&["h"]||22>f&&["hh",f]||1===g&&["d"]||25>=g&&["dd",g]||45>=g&&["M"]||345>g&&["MM",hb(g/30)]||1===h&&["y"]||["yy",h];return i[2]=b,i[3]=a>0,i[4]=c,W.apply({},i)}function Y(a,b,c){var d,e=c-b,f=c-a.day();return f>e&&(f-=7),e-7>f&&(f+=7),d=db(a).add("d",f),{week:Math.ceil(d.dayOfYear()/7),year:d.year()}}function Z(a,b,c,d,e){var f,g,h=U(a,0,1).getUTCDay();return c=null!=c?c:e,f=e-h+(h>d?7:0)-(e>h?7:0),g=7*(b-1)+(c-e)+f+1,{year:g>0?a:a-1,dayOfYear:g>0?g:u(a-1)+g}}function $(a){var b=a._i,c=a._f;return null===b?db.invalid({nullInput:!0}):("string"==typeof b&&(a._i=b=C().preparse(b)),db.isMoment(b)?(a=i(b),a._d=new Date(+b._d)):c?m(c)?Q(a):N(a):S(a),new f(a))}function _(a,b){db.fn[a]=db.fn[a+"s"]=function(a){var c=this._isUTC?"UTC":"";return null!=a?(this._d["set"+c+b](a),db.updateOffset(this),this):this._d["get"+c+b]()}}function ab(a){db.duration.fn[a]=function(){return this._data[a]}}function bb(a,b){db.duration.fn["as"+a]=function(){return+this/b}}function cb(a){var b=!1,c=db;"undefined"==typeof ender&&(a?(gb.moment=function(){return!b&&console&&console.warn&&(b=!0,console.warn("Accessing Moment through the global scope is deprecated, and will be removed in an upcoming release.")),c.apply(null,arguments)},h(gb.moment,c)):gb.moment=db)}for(var db,eb,fb="2.5.1",gb=this,hb=Math.round,ib=0,jb=1,kb=2,lb=3,mb=4,nb=5,ob=6,pb={},qb={_isAMomentObject:null,_i:null,_f:null,_l:null,_strict:null,_isUTC:null,_offset:null,_pf:null,_lang:null},rb="undefined"!=typeof module&&module.exports&&"undefined"!=typeof require,sb=/^\/?Date\((\-?\d+)/i,tb=/(\-)?(?:(\d*)\.)?(\d+)\:(\d+)(?:\:(\d+)\.?(\d{3})?)?/,ub=/^(-)?P(?:(?:([0-9,.]*)Y)?(?:([0-9,.]*)M)?(?:([0-9,.]*)D)?(?:T(?:([0-9,.]*)H)?(?:([0-9,.]*)M)?(?:([0-9,.]*)S)?)?|([0-9,.]*)W)$/,vb=/(\[[^\[]*\])|(\\)?(Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|mm?|ss?|S{1,4}|X|zz?|ZZ?|.)/g,wb=/(\[[^\[]*\])|(\\)?(LT|LL?L?L?|l{1,4})/g,xb=/\d\d?/,yb=/\d{1,3}/,zb=/\d{1,4}/,Ab=/[+\-]?\d{1,6}/,Bb=/\d+/,Cb=/[0-9]*['a-z\u00A0-\u05FF\u0700-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+|[\u0600-\u06FF\/]+(\s*?[\u0600-\u06FF]+){1,2}/i,Db=/Z|[\+\-]\d\d:?\d\d/gi,Eb=/T/i,Fb=/[\+\-]?\d+(\.\d{1,3})?/,Gb=/\d/,Hb=/\d\d/,Ib=/\d{3}/,Jb=/\d{4}/,Kb=/[+-]?\d{6}/,Lb=/[+-]?\d+/,Mb=/^\s*(?:[+-]\d{6}|\d{4})-(?:(\d\d-\d\d)|(W\d\d$)|(W\d\d-\d)|(\d\d\d))((T| )(\d\d(:\d\d(:\d\d(\.\d+)?)?)?)?([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/,Nb="YYYY-MM-DDTHH:mm:ssZ",Ob=[["YYYYYY-MM-DD",/[+-]\d{6}-\d{2}-\d{2}/],["YYYY-MM-DD",/\d{4}-\d{2}-\d{2}/],["GGGG-[W]WW-E",/\d{4}-W\d{2}-\d/],["GGGG-[W]WW",/\d{4}-W\d{2}/],["YYYY-DDD",/\d{4}-\d{3}/]],Pb=[["HH:mm:ss.SSSS",/(T| )\d\d:\d\d:\d\d\.\d{1,3}/],["HH:mm:ss",/(T| )\d\d:\d\d:\d\d/],["HH:mm",/(T| )\d\d:\d\d/],["HH",/(T| )\d\d/]],Qb=/([\+\-]|\d\d)/gi,Rb="Date|Hours|Minutes|Seconds|Milliseconds".split("|"),Sb={Milliseconds:1,Seconds:1e3,Minutes:6e4,Hours:36e5,Days:864e5,Months:2592e6,Years:31536e6},Tb={ms:"millisecond",s:"second",m:"minute",h:"hour",d:"day",D:"date",w:"week",W:"isoWeek",M:"month",y:"year",DDD:"dayOfYear",e:"weekday",E:"isoWeekday",gg:"weekYear",GG:"isoWeekYear"},Ub={dayofyear:"dayOfYear",isoweekday:"isoWeekday",isoweek:"isoWeek",weekyear:"weekYear",isoweekyear:"isoWeekYear"},Vb={},Wb="DDD w W M D d".split(" "),Xb="M D H h m s w W".split(" "),Yb={M:function(){return this.month()+1},MMM:function(a){return this.lang().monthsShort(this,a)},MMMM:function(a){return this.lang().months(this,a)},D:function(){return this.date()},DDD:function(){return this.dayOfYear()},d:function(){return this.day()},dd:function(a){return this.lang().weekdaysMin(this,a)},ddd:function(a){return this.lang().weekdaysShort(this,a)},dddd:function(a){return this.lang().weekdays(this,a)},w:function(){return this.week()},W:function(){return this.isoWeek()},YY:function(){return k(this.year()%100,2)},YYYY:function(){return k(this.year(),4)},YYYYY:function(){return k(this.year(),5)},YYYYYY:function(){var a=this.year(),b=a>=0?"+":"-";return b+k(Math.abs(a),6)},gg:function(){return k(this.weekYear()%100,2)},gggg:function(){return k(this.weekYear(),4)},ggggg:function(){return k(this.weekYear(),5)},GG:function(){return k(this.isoWeekYear()%100,2)},GGGG:function(){return k(this.isoWeekYear(),4)},GGGGG:function(){return k(this.isoWeekYear(),5)},e:function(){return this.weekday()},E:function(){return this.isoWeekday()},a:function(){return this.lang().meridiem(this.hours(),this.minutes(),!0)},A:function(){return this.lang().meridiem(this.hours(),this.minutes(),!1)},H:function(){return this.hours()},h:function(){return this.hours()%12||12},m:function(){return this.minutes()},s:function(){return this.seconds()},S:function(){return s(this.milliseconds()/100)},SS:function(){return k(s(this.milliseconds()/10),2)},SSS:function(){return k(this.milliseconds(),3)},SSSS:function(){return k(this.milliseconds(),3)},Z:function(){var a=-this.zone(),b="+";return 0>a&&(a=-a,b="-"),b+k(s(a/60),2)+":"+k(s(a)%60,2)},ZZ:function(){var a=-this.zone(),b="+";return 0>a&&(a=-a,b="-"),b+k(s(a/60),2)+k(s(a)%60,2)},z:function(){return this.zoneAbbr()},zz:function(){return this.zoneName()},X:function(){return this.unix()},Q:function(){return this.quarter()}},Zb=["months","monthsShort","weekdays","weekdaysShort","weekdaysMin"];Wb.length;)eb=Wb.pop(),Yb[eb+"o"]=d(Yb[eb],eb);for(;Xb.length;)eb=Xb.pop(),Yb[eb+eb]=c(Yb[eb],2);for(Yb.DDDD=c(Yb.DDD,3),h(e.prototype,{set:function(a){var b,c;for(c in a)b=a[c],"function"==typeof b?this[c]=b:this["_"+c]=b},_months:"January_February_March_April_May_June_July_August_September_October_November_December".split("_"),months:function(a){return this._months[a.month()]},_monthsShort:"Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec".split("_"),monthsShort:function(a){return this._monthsShort[a.month()]},monthsParse:function(a){var b,c,d;for(this._monthsParse||(this._monthsParse=[]),b=0;12>b;b++)if(this._monthsParse[b]||(c=db.utc([2e3,b]),d="^"+this.months(c,"")+"|^"+this.monthsShort(c,""),this._monthsParse[b]=new RegExp(d.replace(".",""),"i")),this._monthsParse[b].test(a))return b},_weekdays:"Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday".split("_"),weekdays:function(a){return this._weekdays[a.day()]},_weekdaysShort:"Sun_Mon_Tue_Wed_Thu_Fri_Sat".split("_"),weekdaysShort:function(a){return this._weekdaysShort[a.day()]},_weekdaysMin:"Su_Mo_Tu_We_Th_Fr_Sa".split("_"),weekdaysMin:function(a){return this._weekdaysMin[a.day()]},weekdaysParse:function(a){var b,c,d;for(this._weekdaysParse||(this._weekdaysParse=[]),b=0;7>b;b++)if(this._weekdaysParse[b]||(c=db([2e3,1]).day(b),d="^"+this.weekdays(c,"")+"|^"+this.weekdaysShort(c,"")+"|^"+this.weekdaysMin(c,""),this._weekdaysParse[b]=new RegExp(d.replace(".",""),"i")),this._weekdaysParse[b].test(a))return b},_longDateFormat:{LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D YYYY",LLL:"MMMM D YYYY LT",LLLL:"dddd, MMMM D YYYY LT"},longDateFormat:function(a){var b=this._longDateFormat[a];return!b&&this._longDateFormat[a.toUpperCase()]&&(b=this._longDateFormat[a.toUpperCase()].replace(/MMMM|MM|DD|dddd/g,function(a){return a.slice(1)}),this._longDateFormat[a]=b),b},isPM:function(a){return"p"===(a+"").toLowerCase().charAt(0)},_meridiemParse:/[ap]\.?m?\.?/i,meridiem:function(a,b,c){return a>11?c?"pm":"PM":c?"am":"AM"},_calendar:{sameDay:"[Today at] LT",nextDay:"[Tomorrow at] LT",nextWeek:"dddd [at] LT",lastDay:"[Yesterday at] LT",lastWeek:"[Last] dddd [at] LT",sameElse:"L"},calendar:function(a,b){var c=this._calendar[a];return"function"==typeof c?c.apply(b):c},_relativeTime:{future:"in %s",past:"%s ago",s:"a few seconds",m:"a minute",mm:"%d minutes",h:"an hour",hh:"%d hours",d:"a day",dd:"%d days",M:"a month",MM:"%d months",y:"a year",yy:"%d years"},relativeTime:function(a,b,c,d){var e=this._relativeTime[c];return"function"==typeof e?e(a,b,c,d):e.replace(/%d/i,a)},pastFuture:function(a,b){var c=this._relativeTime[a>0?"future":"past"];return"function"==typeof c?c(b):c.replace(/%s/i,b)},ordinal:function(a){return this._ordinal.replace("%d",a)},_ordinal:"%d",preparse:function(a){return a},postformat:function(a){return a},week:function(a){return Y(a,this._week.dow,this._week.doy).week},_week:{dow:0,doy:6},_invalidDate:"Invalid date",invalidDate:function(){return this._invalidDate}}),db=function(c,d,e,f){var g;return"boolean"==typeof e&&(f=e,e=a),g={},g._isAMomentObject=!0,g._i=c,g._f=d,g._l=e,g._strict=f,g._isUTC=!1,g._pf=b(),$(g)},db.utc=function(c,d,e,f){var g;return"boolean"==typeof e&&(f=e,e=a),g={},g._isAMomentObject=!0,g._useUTC=!0,g._isUTC=!0,g._l=e,g._i=c,g._f=d,g._strict=f,g._pf=b(),$(g).utc()},db.unix=function(a){return db(1e3*a)},db.duration=function(a,b){var c,d,e,f=a,h=null;return db.isDuration(a)?f={ms:a._milliseconds,d:a._days,M:a._months}:"number"==typeof a?(f={},b?f[b]=a:f.milliseconds=a):(h=tb.exec(a))?(c="-"===h[1]?-1:1,f={y:0,d:s(h[kb])*c,h:s(h[lb])*c,m:s(h[mb])*c,s:s(h[nb])*c,ms:s(h[ob])*c}):(h=ub.exec(a))&&(c="-"===h[1]?-1:1,e=function(a){var b=a&&parseFloat(a.replace(",","."));return(isNaN(b)?0:b)*c},f={y:e(h[2]),M:e(h[3]),d:e(h[4]),h:e(h[5]),m:e(h[6]),s:e(h[7]),w:e(h[8])}),d=new g(f),db.isDuration(a)&&a.hasOwnProperty("_lang")&&(d._lang=a._lang),d},db.version=fb,db.defaultFormat=Nb,db.updateOffset=function(){},db.lang=function(a,b){var c;return a?(b?A(y(a),b):null===b?(B(a),a="en"):pb[a]||C(a),c=db.duration.fn._lang=db.fn._lang=C(a),c._abbr):db.fn._lang._abbr},db.langData=function(a){return a&&a._lang&&a._lang._abbr&&(a=a._lang._abbr),C(a)},db.isMoment=function(a){return a instanceof f||null!=a&&a.hasOwnProperty("_isAMomentObject")},db.isDuration=function(a){return a instanceof g},eb=Zb.length-1;eb>=0;--eb)r(Zb[eb]);for(db.normalizeUnits=function(a){return p(a)},db.invalid=function(a){var b=db.utc(0/0);return null!=a?h(b._pf,a):b._pf.userInvalidated=!0,b},db.parseZone=function(a){return db(a).parseZone()},h(db.fn=f.prototype,{clone:function(){return db(this)},valueOf:function(){return+this._d+6e4*(this._offset||0)},unix:function(){return Math.floor(+this/1e3)},toString:function(){return this.clone().lang("en").format("ddd MMM DD YYYY HH:mm:ss [GMT]ZZ")},toDate:function(){return this._offset?new Date(+this):this._d},toISOString:function(){var a=db(this).utc();return 0<a.year()&&a.year()<=9999?F(a,"YYYY-MM-DD[T]HH:mm:ss.SSS[Z]"):F(a,"YYYYYY-MM-DD[T]HH:mm:ss.SSS[Z]")},toArray:function(){var a=this;return[a.year(),a.month(),a.date(),a.hours(),a.minutes(),a.seconds(),a.milliseconds()]},isValid:function(){return x(this)},isDSTShifted:function(){return this._a?this.isValid()&&o(this._a,(this._isUTC?db.utc(this._a):db(this._a)).toArray())>0:!1},parsingFlags:function(){return h({},this._pf)},invalidAt:function(){return this._pf.overflow},utc:function(){return this.zone(0)},local:function(){return this.zone(0),this._isUTC=!1,this},format:function(a){var b=F(this,a||db.defaultFormat);return this.lang().postformat(b)},add:function(a,b){var c;return c="string"==typeof a?db.duration(+b,a):db.duration(a,b),l(this,c,1),this},subtract:function(a,b){var c;return c="string"==typeof a?db.duration(+b,a):db.duration(a,b),l(this,c,-1),this},diff:function(a,b,c){var d,e,f=z(a,this),g=6e4*(this.zone()-f.zone());return b=p(b),"year"===b||"month"===b?(d=432e5*(this.daysInMonth()+f.daysInMonth()),e=12*(this.year()-f.year())+(this.month()-f.month()),e+=(this-db(this).startOf("month")-(f-db(f).startOf("month")))/d,e-=6e4*(this.zone()-db(this).startOf("month").zone()-(f.zone()-db(f).startOf("month").zone()))/d,"year"===b&&(e/=12)):(d=this-f,e="second"===b?d/1e3:"minute"===b?d/6e4:"hour"===b?d/36e5:"day"===b?(d-g)/864e5:"week"===b?(d-g)/6048e5:d),c?e:j(e)},from:function(a,b){return db.duration(this.diff(a)).lang(this.lang()._abbr).humanize(!b)},fromNow:function(a){return this.from(db(),a)},calendar:function(){var a=z(db(),this).startOf("day"),b=this.diff(a,"days",!0),c=-6>b?"sameElse":-1>b?"lastWeek":0>b?"lastDay":1>b?"sameDay":2>b?"nextDay":7>b?"nextWeek":"sameElse";return this.format(this.lang().calendar(c,this))},isLeapYear:function(){return v(this.year())},isDST:function(){return this.zone()<this.clone().month(0).zone()||this.zone()<this.clone().month(5).zone()},day:function(a){var b=this._isUTC?this._d.getUTCDay():this._d.getDay();return null!=a?(a=V(a,this.lang()),this.add({d:a-b})):b},month:function(a){var b,c=this._isUTC?"UTC":"";return null!=a?"string"==typeof a&&(a=this.lang().monthsParse(a),"number"!=typeof a)?this:(b=this.date(),this.date(1),this._d["set"+c+"Month"](a),this.date(Math.min(b,this.daysInMonth())),db.updateOffset(this),this):this._d["get"+c+"Month"]()},startOf:function(a){switch(a=p(a)){case"year":this.month(0);case"month":this.date(1);case"week":case"isoWeek":case"day":this.hours(0);case"hour":this.minutes(0);case"minute":this.seconds(0);case"second":this.milliseconds(0)}return"week"===a?this.weekday(0):"isoWeek"===a&&this.isoWeekday(1),this},endOf:function(a){return a=p(a),this.startOf(a).add("isoWeek"===a?"week":a,1).subtract("ms",1)},isAfter:function(a,b){return b="undefined"!=typeof b?b:"millisecond",+this.clone().startOf(b)>+db(a).startOf(b)},isBefore:function(a,b){return b="undefined"!=typeof b?b:"millisecond",+this.clone().startOf(b)<+db(a).startOf(b)},isSame:function(a,b){return b=b||"ms",+this.clone().startOf(b)===+z(a,this).startOf(b)},min:function(a){return a=db.apply(null,arguments),this>a?this:a},max:function(a){return a=db.apply(null,arguments),a>this?this:a},zone:function(a){var b=this._offset||0;return null==a?this._isUTC?b:this._d.getTimezoneOffset():("string"==typeof a&&(a=I(a)),Math.abs(a)<16&&(a=60*a),this._offset=a,this._isUTC=!0,b!==a&&l(this,db.duration(b-a,"m"),1,!0),this)},zoneAbbr:function(){return this._isUTC?"UTC":""},zoneName:function(){return this._isUTC?"Coordinated Universal Time":""},parseZone:function(){return this._tzm?this.zone(this._tzm):"string"==typeof this._i&&this.zone(this._i),this},hasAlignedHourOffset:function(a){return a=a?db(a).zone():0,(this.zone()-a)%60===0},daysInMonth:function(){return t(this.year(),this.month())},dayOfYear:function(a){var b=hb((db(this).startOf("day")-db(this).startOf("year"))/864e5)+1;return null==a?b:this.add("d",a-b)},quarter:function(){return Math.ceil((this.month()+1)/3)},weekYear:function(a){var b=Y(this,this.lang()._week.dow,this.lang()._week.doy).year;return null==a?b:this.add("y",a-b)},isoWeekYear:function(a){var b=Y(this,1,4).year;return null==a?b:this.add("y",a-b)},week:function(a){var b=this.lang().week(this);return null==a?b:this.add("d",7*(a-b))},isoWeek:function(a){var b=Y(this,1,4).week;return null==a?b:this.add("d",7*(a-b))},weekday:function(a){var b=(this.day()+7-this.lang()._week.dow)%7;return null==a?b:this.add("d",a-b)},isoWeekday:function(a){return null==a?this.day()||7:this.day(this.day()%7?a:a-7)},get:function(a){return a=p(a),this[a]()},set:function(a,b){return a=p(a),"function"==typeof this[a]&&this[a](b),this},lang:function(b){return b===a?this._lang:(this._lang=C(b),this)}}),eb=0;eb<Rb.length;eb++)_(Rb[eb].toLowerCase().replace(/s$/,""),Rb[eb]);_("year","FullYear"),db.fn.days=db.fn.day,db.fn.months=db.fn.month,db.fn.weeks=db.fn.week,db.fn.isoWeeks=db.fn.isoWeek,db.fn.toJSON=db.fn.toISOString,h(db.duration.fn=g.prototype,{_bubble:function(){var a,b,c,d,e=this._milliseconds,f=this._days,g=this._months,h=this._data;h.milliseconds=e%1e3,a=j(e/1e3),h.seconds=a%60,b=j(a/60),h.minutes=b%60,c=j(b/60),h.hours=c%24,f+=j(c/24),h.days=f%30,g+=j(f/30),h.months=g%12,d=j(g/12),h.years=d},weeks:function(){return j(this.days()/7)},valueOf:function(){return this._milliseconds+864e5*this._days+this._months%12*2592e6+31536e6*s(this._months/12)},humanize:function(a){var b=+this,c=X(b,!a,this.lang());return a&&(c=this.lang().pastFuture(b,c)),this.lang().postformat(c)},add:function(a,b){var c=db.duration(a,b);return this._milliseconds+=c._milliseconds,this._days+=c._days,this._months+=c._months,this._bubble(),this},subtract:function(a,b){var c=db.duration(a,b);return this._milliseconds-=c._milliseconds,this._days-=c._days,this._months-=c._months,this._bubble(),this},get:function(a){return a=p(a),this[a.toLowerCase()+"s"]()},as:function(a){return a=p(a),this["as"+a.charAt(0).toUpperCase()+a.slice(1)+"s"]()},lang:db.fn.lang,toIsoString:function(){var a=Math.abs(this.years()),b=Math.abs(this.months()),c=Math.abs(this.days()),d=Math.abs(this.hours()),e=Math.abs(this.minutes()),f=Math.abs(this.seconds()+this.milliseconds()/1e3);return this.asSeconds()?(this.asSeconds()<0?"-":"")+"P"+(a?a+"Y":"")+(b?b+"M":"")+(c?c+"D":"")+(d||e||f?"T":"")+(d?d+"H":"")+(e?e+"M":"")+(f?f+"S":""):"P0D"}});for(eb in Sb)Sb.hasOwnProperty(eb)&&(bb(eb,Sb[eb]),ab(eb.toLowerCase()));bb("Weeks",6048e5),db.duration.fn.asMonths=function(){return(+this-31536e6*this.years())/2592e6+12*this.years()},db.lang("en",{ordinal:function(a){var b=a%10,c=1===s(a%100/10)?"th":1===b?"st":2===b?"nd":3===b?"rd":"th";return a+c}}),rb?(module.exports=db,cb(!0)):"function"==typeof define&&define.amd?define("moment",function(b,c,d){return d.config&&d.config()&&d.config().noGlobal!==!0&&cb(d.config().noGlobal===a),db}):cb()}).call(this);
/* daterangepicker.js */!function(b,c){var a=function(g,f,d){this.parentEl="body";this.element=b(g);var e='<div class="daterangepicker dropdown-menu"><div class="calendar left"></div><div class="calendar right"></div><div class="ranges"><div class="range_inputs"><div class="daterangepicker_start_input"><label for="daterangepicker_start"></label><input class="input-mini" type="text" name="daterangepicker_start" value="" disabled="disabled" /></div><div class="daterangepicker_end_input"><label for="daterangepicker_end"></label><input class="input-mini" type="text" name="daterangepicker_end" value="" disabled="disabled" /></div><button class="applyBtn" disabled="disabled"></button>&nbsp;<button class="cancelBtn"></button></div></div></div>';if(typeof f!=="object"||f===null){f={}}this.parentEl=(typeof f==="object"&&f.parentEl&&b(f.parentEl).length)?b(f.parentEl):b(this.parentEl);this.container=b(e).appendTo(this.parentEl);this.setOptions(f,d);var h=this.container;b.each(this.buttonClasses,function(i,j){h.find("button").addClass(j)});this.container.find(".daterangepicker_start_input label").html(this.locale.fromLabel);this.container.find(".daterangepicker_end_input label").html(this.locale.toLabel);if(this.applyClass.length){this.container.find(".applyBtn").addClass(this.applyClass)}if(this.cancelClass.length){this.container.find(".cancelBtn").addClass(this.cancelClass)}this.container.find(".applyBtn").html(this.locale.applyLabel);this.container.find(".cancelBtn").html(this.locale.cancelLabel);this.container.find(".calendar").on("click.daterangepicker",".prev",b.proxy(this.clickPrev,this)).on("click.daterangepicker",".next",b.proxy(this.clickNext,this)).on("click.daterangepicker","td.available",b.proxy(this.clickDate,this)).on("mouseenter.daterangepicker","td.available",b.proxy(this.enterDate,this)).on("mouseleave.daterangepicker","td.available",b.proxy(this.updateFormInputs,this)).on("change.daterangepicker","select.yearselect",b.proxy(this.updateMonthYear,this)).on("change.daterangepicker","select.monthselect",b.proxy(this.updateMonthYear,this)).on("change.daterangepicker","select.hourselect,select.minuteselect,select.ampmselect",b.proxy(this.updateTime,this));this.container.find(".ranges").on("click.daterangepicker","button.applyBtn",b.proxy(this.clickApply,this)).on("click.daterangepicker","button.cancelBtn",b.proxy(this.clickCancel,this)).on("click.daterangepicker",".daterangepicker_start_input,.daterangepicker_end_input",b.proxy(this.showCalendars,this)).on("click.daterangepicker","li",b.proxy(this.clickRange,this)).on("mouseenter.daterangepicker","li",b.proxy(this.enterRange,this)).on("mouseleave.daterangepicker","li",b.proxy(this.updateFormInputs,this));if(this.element.is("input")){this.element.on({"click.daterangepicker":b.proxy(this.show,this),"focus.daterangepicker":b.proxy(this.show,this),"keyup.daterangepicker":b.proxy(this.updateFromControl,this)})}else{this.element.on("click.daterangepicker",b.proxy(this.toggle,this))}};a.prototype={constructor:a,setOptions:function(n,m){this.startDate=c().startOf("day");this.endDate=c().endOf("day");this.minDate=false;this.maxDate=false;this.dateLimit=false;this.showDropdowns=false;this.showWeekNumbers=false;this.timePicker=false;this.timePickerIncrement=30;this.timePicker12Hour=true;this.singleDatePicker=false;this.ranges={};this.opens="right";if(this.element.hasClass("pull-right")){this.opens="left"}this.buttonClasses=["btn","btn-small"];this.applyClass="btn-success";this.cancelClass="btn-default";this.format="MM/DD/YYYY";this.separator=" - ";this.locale={applyLabel:"Apply",cancelLabel:"Cancel",fromLabel:"From",toLabel:"To",weekLabel:"W",customRangeLabel:"Custom Range",daysOfWeek:c()._lang._weekdaysMin.slice(),monthNames:c()._lang._monthsShort.slice(),firstDay:0};this.cb=function(){};if(typeof n.format==="string"){this.format=n.format}if(typeof n.separator==="string"){this.separator=n.separator}if(typeof n.startDate==="string"){this.startDate=c(n.startDate,this.format)}if(typeof n.endDate==="string"){this.endDate=c(n.endDate,this.format)}if(typeof n.minDate==="string"){this.minDate=c(n.minDate,this.format)}if(typeof n.maxDate==="string"){this.maxDate=c(n.maxDate,this.format)}if(typeof n.startDate==="object"){this.startDate=c(n.startDate)}if(typeof n.endDate==="object"){this.endDate=c(n.endDate)}if(typeof n.minDate==="object"){this.minDate=c(n.minDate)}if(typeof n.maxDate==="object"){this.maxDate=c(n.maxDate)}if(typeof n.applyClass==="string"){this.applyClass=n.applyClass}if(typeof n.cancelClass==="string"){this.cancelClass=n.cancelClass}if(typeof n.dateLimit==="object"){this.dateLimit=n.dateLimit}if(typeof n.locale==="object"){if(typeof n.locale.daysOfWeek==="object"){this.locale.daysOfWeek=n.locale.daysOfWeek.slice()}if(typeof n.locale.monthNames==="object"){this.locale.monthNames=n.locale.monthNames.slice()}if(typeof n.locale.firstDay==="number"){this.locale.firstDay=n.locale.firstDay;var h=n.locale.firstDay;while(h>0){this.locale.daysOfWeek.push(this.locale.daysOfWeek.shift());h--}}if(typeof n.locale.applyLabel==="string"){this.locale.applyLabel=n.locale.applyLabel}if(typeof n.locale.cancelLabel==="string"){this.locale.cancelLabel=n.locale.cancelLabel}if(typeof n.locale.fromLabel==="string"){this.locale.fromLabel=n.locale.fromLabel}if(typeof n.locale.toLabel==="string"){this.locale.toLabel=n.locale.toLabel}if(typeof n.locale.weekLabel==="string"){this.locale.weekLabel=n.locale.weekLabel}if(typeof n.locale.customRangeLabel==="string"){this.locale.customRangeLabel=n.locale.customRangeLabel}}if(typeof n.opens==="string"){this.opens=n.opens}if(typeof n.showWeekNumbers==="boolean"){this.showWeekNumbers=n.showWeekNumbers}if(typeof n.buttonClasses==="string"){this.buttonClasses=[n.buttonClasses]}if(typeof n.buttonClasses==="object"){this.buttonClasses=n.buttonClasses}if(typeof n.showDropdowns==="boolean"){this.showDropdowns=n.showDropdowns}if(typeof n.singleDatePicker==="boolean"){this.singleDatePicker=n.singleDatePicker}if(typeof n.timePicker==="boolean"){this.timePicker=n.timePicker}if(typeof n.timePickerIncrement==="number"){this.timePickerIncrement=n.timePickerIncrement}if(typeof n.timePicker12Hour==="boolean"){this.timePicker12Hour=n.timePicker12Hour}var d,g,i;if(typeof n.startDate==="undefined"&&typeof n.endDate==="undefined"){if(b(this.element).is("input[type=text]")){var e=b(this.element).val();var k=e.split(this.separator);d=g=null;if(k.length==2){d=c(k[0],this.format);g=c(k[1],this.format)}else{if(this.singleDatePicker){d=c(e,this.format);g=c(e,this.format)}}if(d!==null&&g!==null){this.startDate=d;this.endDate=g}}}if(typeof n.ranges==="object"){for(i in n.ranges){d=c(n.ranges[i][0]);g=c(n.ranges[i][1]);if(this.minDate&&d.isBefore(this.minDate)){d=c(this.minDate)}if(this.maxDate&&g.isAfter(this.maxDate)){g=c(this.maxDate)}if((this.minDate&&g.isBefore(this.minDate))||(this.maxDate&&d.isAfter(this.maxDate))){continue}this.ranges[i]=[d,g]}var j="<ul>";for(i in this.ranges){j+="<li>"+i+"</li>"}j+="<li>"+this.locale.customRangeLabel+"</li>";j+="</ul>";this.container.find(".ranges ul").remove();this.container.find(".ranges").prepend(j)}if(typeof m==="function"){this.cb=m}if(!this.timePicker){this.startDate=this.startDate.startOf("day");this.endDate=this.endDate.endOf("day")}if(this.singleDatePicker){this.opens="right";this.container.find(".calendar.right").show();this.container.find(".calendar.left").hide();this.container.find(".ranges").hide();if(!this.container.find(".calendar.right").hasClass("single")){this.container.find(".calendar.right").addClass("single")}}else{this.container.find(".calendar.right").removeClass("single");this.container.find(".ranges").show()}this.oldStartDate=this.startDate.clone();this.oldEndDate=this.endDate.clone();this.oldChosenLabel=this.chosenLabel;this.leftCalendar={month:c([this.startDate.year(),this.startDate.month(),1,this.startDate.hour(),this.startDate.minute()]),calendar:[]};this.rightCalendar={month:c([this.endDate.year(),this.endDate.month(),1,this.endDate.hour(),this.endDate.minute()]),calendar:[]};if(this.opens=="right"){var f=this.container.find(".calendar.left");var l=this.container.find(".calendar.right");f.removeClass("left").addClass("right");l.removeClass("right").addClass("left")}if(typeof n.ranges==="undefined"&&!this.singleDatePicker){this.container.addClass("show-calendar")}this.container.addClass("opens"+this.opens);this.updateView();this.updateCalendars()},setStartDate:function(d){if(typeof d==="string"){this.startDate=c(d,this.format)}if(typeof d==="object"){this.startDate=c(d)}if(!this.timePicker){this.startDate=this.startDate.startOf("day")}this.oldStartDate=this.startDate.clone();this.updateView();this.updateCalendars()},setEndDate:function(d){if(typeof d==="string"){this.endDate=c(d,this.format)}if(typeof d==="object"){this.endDate=c(d)}if(!this.timePicker){this.endDate=this.endDate.endOf("day")}this.oldEndDate=this.endDate.clone();this.updateView();this.updateCalendars()},updateView:function(){this.leftCalendar.month.month(this.startDate.month()).year(this.startDate.year());this.rightCalendar.month.month(this.endDate.month()).year(this.endDate.year());this.updateFormInputs()},updateFormInputs:function(){this.container.find("input[name=daterangepicker_start]").val(this.startDate.format(this.format));this.container.find("input[name=daterangepicker_end]").val(this.endDate.format(this.format));if(this.startDate.isSame(this.endDate)||this.startDate.isBefore(this.endDate)){this.container.find("button.applyBtn").removeAttr("disabled")}else{this.container.find("button.applyBtn").attr("disabled","disabled")}},updateFromControl:function(){if(!this.element.is("input")){return}if(!this.element.val().length){return}var e=this.element.val().split(this.separator),f=null,d=null;if(e.length===2){f=c(e[0],this.format);d=c(e[1],this.format)}if(this.singleDatePicker||f===null||d===null){f=c(this.element.val(),this.format);d=f}if(d.isBefore(f)){return}this.oldStartDate=this.startDate.clone();this.oldEndDate=this.endDate.clone();this.startDate=f;this.endDate=d;if(!this.startDate.isSame(this.oldStartDate)||!this.endDate.isSame(this.oldEndDate)){this.notify()}this.updateCalendars()},notify:function(){this.updateView();this.cb(this.startDate,this.endDate,this.chosenLabel)},move:function(){var d={top:0,left:0};if(!this.parentEl.is("body")){d={top:this.parentEl.offset().top-this.parentEl.scrollTop(),left:this.parentEl.offset().left-this.parentEl.scrollLeft()}}if(this.opens=="left"){this.container.css({top:this.element.offset().top+this.element.outerHeight()-d.top,right:b(window).width()-this.element.offset().left-this.element.outerWidth()-d.left,left:"auto"});if(this.container.offset().left<0){this.container.css({right:"auto",left:9})}}else{this.container.css({top:this.element.offset().top+this.element.outerHeight()-d.top,left:this.element.offset().left-d.left,right:"auto"});if(this.container.offset().left+this.container.outerWidth()>b(window).width()){this.container.css({left:"auto",right:0})}}},toggle:function(d){if(this.element.hasClass("active")){this.hide()}else{this.show()}},show:function(d){this.element.addClass("active");this.container.show();this.move();this._outsideClickProxy=b.proxy(function(f){this.outsideClick(f)},this);b(document).on("mousedown.daterangepicker",this._outsideClickProxy).on("click.daterangepicker","[data-toggle=dropdown]",this._outsideClickProxy).on("focusin.daterangepicker",this._outsideClickProxy);this.element.trigger("show.daterangepicker",this)},outsideClick:function(f){var d=b(f.target);if(d.closest(this.element).length||d.closest(this.container).length||d.closest(".calendar-date").length){return}this.hide()},hide:function(d){b(document).off("mousedown.daterangepicker",this._outsideClickProxy).off("click.daterangepicker",this._outsideClickProxy).off("focusin.daterangepicker",this._outsideClickProxy);this.element.removeClass("active");this.container.hide();if(!this.startDate.isSame(this.oldStartDate)||!this.endDate.isSame(this.oldEndDate)){this.notify()}this.oldStartDate=this.startDate.clone();this.oldEndDate=this.endDate.clone();this.element.trigger("hide.daterangepicker",this)},enterRange:function(g){var d=g.target.innerHTML;if(d==this.locale.customRangeLabel){this.updateView()}else{var f=this.ranges[d];this.container.find("input[name=daterangepicker_start]").val(f[0].format(this.format));this.container.find("input[name=daterangepicker_end]").val(f[1].format(this.format))}},showCalendars:function(){this.container.addClass("show-calendar");this.move()},hideCalendars:function(){this.container.removeClass("show-calendar")},updateInputText:function(){if(this.element.is("input")&&!this.singleDatePicker){this.element.val(this.startDate.format(this.format)+this.separator+this.endDate.format(this.format))}else{if(this.element.is("input")){this.element.val(this.startDate.format(this.format))}}},clickRange:function(g){var d=g.target.innerHTML;this.chosenLabel=d;if(d==this.locale.customRangeLabel){this.showCalendars()}else{var f=this.ranges[d];this.startDate=f[0];this.endDate=f[1];if(!this.timePicker){this.startDate.startOf("day");this.endDate.endOf("day")}this.leftCalendar.month.month(this.startDate.month()).year(this.startDate.year()).hour(this.startDate.hour()).minute(this.startDate.minute());this.rightCalendar.month.month(this.endDate.month()).year(this.endDate.year()).hour(this.endDate.hour()).minute(this.endDate.minute());this.updateCalendars();this.updateInputText();this.hideCalendars();this.hide();this.element.trigger("apply.daterangepicker",this)}},clickPrev:function(f){var d=b(f.target).parents(".calendar");if(d.hasClass("left")){this.leftCalendar.month.subtract("month",1)}else{this.rightCalendar.month.subtract("month",1)}this.updateCalendars()},clickNext:function(f){var d=b(f.target).parents(".calendar");if(d.hasClass("left")){this.leftCalendar.month.add("month",1)}else{this.rightCalendar.month.add("month",1)}this.updateCalendars()},enterDate:function(g){var i=b(g.target).attr("data-title");var h=i.substr(1,1);var d=i.substr(3,1);var f=b(g.target).parents(".calendar");if(f.hasClass("left")){this.container.find("input[name=daterangepicker_start]").val(this.leftCalendar.calendar[h][d].format(this.format))}else{this.container.find("input[name=daterangepicker_end]").val(this.rightCalendar.calendar[h][d].format(this.format))}},clickDate:function(l){var m=b(l.target).attr("data-title");var n=m.substr(1,1);var h=m.substr(3,1);var f=b(l.target).parents(".calendar");var g,k;if(f.hasClass("left")){g=this.leftCalendar.calendar[n][h];k=this.endDate;if(typeof this.dateLimit==="object"){var d=c(g).add(this.dateLimit).startOf("day");if(k.isAfter(d)){k=d}}}else{g=this.startDate;k=this.rightCalendar.calendar[n][h];if(typeof this.dateLimit==="object"){var j=c(k).subtract(this.dateLimit).startOf("day");if(g.isBefore(j)){g=j}}}if(this.singleDatePicker&&f.hasClass("left")){k=g.clone()}else{if(this.singleDatePicker&&f.hasClass("right")){g=k.clone()}}f.find("td").removeClass("active");if(g.isSame(k)||g.isBefore(k)){b(l.target).addClass("active");this.startDate=g;this.endDate=k;this.chosenLabel=this.locale.customRangeLabel}else{if(g.isAfter(k)){b(l.target).addClass("active");var i=this.endDate.diff(this.startDate);this.startDate=g;this.endDate=c(g).add("ms",i);this.chosenLabel=this.locale.customRangeLabel}}this.leftCalendar.month.month(this.startDate.month()).year(this.startDate.year());this.rightCalendar.month.month(this.endDate.month()).year(this.endDate.year());this.updateCalendars();if(!this.timePicker){k.endOf("day")}if(this.singleDatePicker){this.clickApply()}},clickApply:function(d){this.updateInputText();this.hide();this.element.trigger("apply.daterangepicker",this)},clickCancel:function(d){this.startDate=this.oldStartDate;this.endDate=this.oldEndDate;this.chosenLabel=this.oldChosenLabel;this.updateView();this.updateCalendars();this.hide();this.element.trigger("cancel.daterangepicker",this)},updateMonthYear:function(h){var j=b(h.target).closest(".calendar").hasClass("left"),i=j?"left":"right",g=this.container.find(".calendar."+i);var f=parseInt(g.find(".monthselect").val(),10);var d=g.find(".yearselect").val();this[i+"Calendar"].month.month(f).year(d);this.updateCalendars()},updateTime:function(i){var h=b(i.target).closest(".calendar"),k=h.hasClass("left");var f=parseInt(h.find(".hourselect").val(),10);var j=parseInt(h.find(".minuteselect").val(),10);if(this.timePicker12Hour){var g=h.find(".ampmselect").val();if(g==="PM"&&f<12){f+=12}if(g==="AM"&&f===12){f=0}}if(k){var l=this.startDate.clone();l.hour(f);l.minute(j);this.startDate=l;this.leftCalendar.month.hour(f).minute(j)}else{var d=this.endDate.clone();d.hour(f);d.minute(j);this.endDate=d;this.rightCalendar.month.hour(f).minute(j)}this.updateCalendars()},updateCalendars:function(){this.leftCalendar.calendar=this.buildCalendar(this.leftCalendar.month.month(),this.leftCalendar.month.year(),this.leftCalendar.month.hour(),this.leftCalendar.month.minute(),"left");this.rightCalendar.calendar=this.buildCalendar(this.rightCalendar.month.month(),this.rightCalendar.month.year(),this.rightCalendar.month.hour(),this.rightCalendar.month.minute(),"right");this.container.find(".calendar.left").empty().html(this.renderCalendar(this.leftCalendar.calendar,this.startDate,this.minDate,this.maxDate));this.container.find(".calendar.right").empty().html(this.renderCalendar(this.rightCalendar.calendar,this.endDate,this.startDate,this.maxDate));this.container.find(".ranges li").removeClass("active");var d=true;var f=0;for(var e in this.ranges){if(this.timePicker){if(this.startDate.isSame(this.ranges[e][0])&&this.endDate.isSame(this.ranges[e][1])){d=false;this.chosenLabel=this.container.find(".ranges li:eq("+f+")").addClass("active").html()}}else{if(this.startDate.format("YYYY-MM-DD")==this.ranges[e][0].format("YYYY-MM-DD")&&this.endDate.format("YYYY-MM-DD")==this.ranges[e][1].format("YYYY-MM-DD")){d=false;this.chosenLabel=this.container.find(".ranges li:eq("+f+")").addClass("active").html()}}f++}if(d){this.chosenLabel=this.container.find(".ranges li:last").addClass("active").html()}},buildCalendar:function(p,r,j,h,q){var d=c([r,p,1]);var n=c(d).subtract("month",1).month();var m=c(d).subtract("month",1).year();var s=c([m,n]).daysInMonth();var f=d.day();var l;var k=[];for(l=0;l<6;l++){k[l]=[]}var o=s-f+this.locale.firstDay+1;if(o>s){o-=7}if(f==this.locale.firstDay){o=s-6}var g=c([m,n,o,12,h]);var e,t;for(l=0,e=0,t=0;l<42;l++,e++,g=c(g).add("hour",24)){if(l>0&&e%7===0){e=0;t++}k[t][e]=g.clone().hour(j);g.hour(12)}return k},renderDropdowns:function(i,h,d){var l=i.month();var g='<select class="monthselect">';var e=false;var q=false;for(var f=0;f<12;f++){if((!e||f>=h.month())&&(!q||f<=d.month())){g+="<option value='"+f+"'"+(f===l?" selected='selected'":"")+">"+this.locale.monthNames[f]+"</option>"}}g+="</select>";var k=i.year();var j=(d&&d.year())||(k+5);var p=(h&&h.year())||(k-50);var o='<select class="yearselect">';for(var n=p;n<=j;n++){o+='<option value="'+n+'"'+(n===k?' selected="selected"':"")+">"+n+"</option>"}o+="</select>";return g+o},renderCalendar:function(n,l,k,e){var q='<div class="calendar-date">';q+='<table class="table-condensed">';q+="<thead>";q+="<tr>";if(this.showWeekNumbers){q+="<th></th>"}if(!k||k.isBefore(n[1][1])){q+='<th class="prev available"><i class="fa fa-arrow-left icon-arrow-left glyphicon glyphicon-arrow-left"></i></th>'}else{q+="<th></th>"}var g=this.locale.monthNames[n[1][1].month()]+n[1][1].format(" YYYY");if(this.showDropdowns){g=this.renderDropdowns(n[1][1],k,e)}q+='<th colspan="5" class="month">'+g+"</th>";if(!e||e.isAfter(n[1][1])){q+='<th class="next available"><i class="fa fa-arrow-right icon-arrow-right glyphicon glyphicon-arrow-right"></i></th>'}else{q+="<th></th>"}q+="</tr>";q+="<tr>";if(this.showWeekNumbers){q+='<th class="week">'+this.locale.weekLabel+"</th>"}b.each(this.locale.daysOfWeek,function(t,i){q+="<th>"+i+"</th>"});q+="</tr>";q+="</thead>";q+="<tbody>";for(var s=0;s<6;s++){q+="<tr>";if(this.showWeekNumbers){q+='<td class="week">'+n[s][0].week()+"</td>"}for(var h=0;h<7;h++){var p="available ";p+=(n[s][h].month()==n[1][1].month())?"":"off";if((k&&n[s][h].isBefore(k,"day"))||(e&&n[s][h].isAfter(e,"day"))){p=" off disabled "}else{if(n[s][h].format("YYYY-MM-DD")==l.format("YYYY-MM-DD")){p+=" active ";if(n[s][h].format("YYYY-MM-DD")==this.startDate.format("YYYY-MM-DD")){p+=" start-date "}if(n[s][h].format("YYYY-MM-DD")==this.endDate.format("YYYY-MM-DD")){p+=" end-date "}}else{if(n[s][h]>=this.startDate&&n[s][h]<=this.endDate){p+=" in-range ";if(n[s][h].isSame(this.startDate)){p+=" start-date "}if(n[s][h].isSame(this.endDate)){p+=" end-date "}}}}var r="r"+s+"c"+h;q+='<td class="'+p.replace(/\s+/g," ").replace(/^\s?(.*?)\s?$/,"$1")+'" data-title="'+r+'">'+n[s][h].date()+"</td>"}q+="</tr>"}q+="</tbody>";q+="</table>";q+="</div>";var m;if(this.timePicker){q+='<div class="calendar-time">';q+='<select class="hourselect">';var f=0;var j=23;var d=l.hour();if(this.timePicker12Hour){f=1;j=12;if(d>=12){d-=12}if(d===0){d=12}}for(m=f;m<=j;m++){if(m==d){q+='<option value="'+m+'" selected="selected">'+m+"</option>"}else{q+='<option value="'+m+'">'+m+"</option>"}}q+="</select> : ";q+='<select class="minuteselect">';for(m=0;m<60;m+=this.timePickerIncrement){var o=m;if(o<10){o="0"+o}if(m==l.minute()){q+='<option value="'+m+'" selected="selected">'+o+"</option>"}else{q+='<option value="'+m+'">'+o+"</option>"}}q+="</select> ";if(this.timePicker12Hour){q+='<select class="ampmselect">';if(l.hour()>=12){q+='<option value="AM">AM</option><option value="PM" selected="selected">PM</option>'}else{q+='<option value="AM" selected="selected">AM</option><option value="PM">PM</option>'}q+="</select>"}q+="</div>"}return q},remove:function(){this.container.remove();this.element.off(".daterangepicker");this.element.removeData("daterangepicker")}};b.fn.daterangepicker=function(e,d){this.each(function(){var f=b(this);if(f.data("daterangepicker")){f.data("daterangepicker").remove()}f.data("daterangepicker",new a(f,e,d))});return this}}(window.jQuery,window.moment);

/** Colorpicker v2.0
	http://mjolnic.github.io/bootstrap-colorpicker/
 **************************************************************** **/
!function(a){"use strict";"function"==typeof define&&define.amd?define(["jquery"],a):window.jQuery&&!window.jQuery.fn.colorpicker&&a(window.jQuery)}(function(a){"use strict";var b=function(a){this.value={h:0,s:0,b:0,a:1},this.origFormat=null,a&&(void 0!==a.toLowerCase?this.setColor(a):void 0!==a.h&&(this.value=a))};b.prototype={constructor:b,colors:{aliceblue:"#f0f8ff",antiquewhite:"#faebd7",aqua:"#00ffff",aquamarine:"#7fffd4",azure:"#f0ffff",beige:"#f5f5dc",bisque:"#ffe4c4",black:"#000000",blanchedalmond:"#ffebcd",blue:"#0000ff",blueviolet:"#8a2be2",brown:"#a52a2a",burlywood:"#deb887",cadetblue:"#5f9ea0",chartreuse:"#7fff00",chocolate:"#d2691e",coral:"#ff7f50",cornflowerblue:"#6495ed",cornsilk:"#fff8dc",crimson:"#dc143c",cyan:"#00ffff",darkblue:"#00008b",darkcyan:"#008b8b",darkgoldenrod:"#b8860b",darkgray:"#a9a9a9",darkgreen:"#006400",darkkhaki:"#bdb76b",darkmagenta:"#8b008b",darkolivegreen:"#556b2f",darkorange:"#ff8c00",darkorchid:"#9932cc",darkred:"#8b0000",darksalmon:"#e9967a",darkseagreen:"#8fbc8f",darkslateblue:"#483d8b",darkslategray:"#2f4f4f",darkturquoise:"#00ced1",darkviolet:"#9400d3",deeppink:"#ff1493",deepskyblue:"#00bfff",dimgray:"#696969",dodgerblue:"#1e90ff",firebrick:"#b22222",floralwhite:"#fffaf0",forestgreen:"#228b22",fuchsia:"#ff00ff",gainsboro:"#dcdcdc",ghostwhite:"#f8f8ff",gold:"#ffd700",goldenrod:"#daa520",gray:"#808080",green:"#008000",greenyellow:"#adff2f",honeydew:"#f0fff0",hotpink:"#ff69b4","indianred ":"#cd5c5c","indigo ":"#4b0082",ivory:"#fffff0",khaki:"#f0e68c",lavender:"#e6e6fa",lavenderblush:"#fff0f5",lawngreen:"#7cfc00",lemonchiffon:"#fffacd",lightblue:"#add8e6",lightcoral:"#f08080",lightcyan:"#e0ffff",lightgoldenrodyellow:"#fafad2",lightgrey:"#d3d3d3",lightgreen:"#90ee90",lightpink:"#ffb6c1",lightsalmon:"#ffa07a",lightseagreen:"#20b2aa",lightskyblue:"#87cefa",lightslategray:"#778899",lightsteelblue:"#b0c4de",lightyellow:"#ffffe0",lime:"#00ff00",limegreen:"#32cd32",linen:"#faf0e6",magenta:"#ff00ff",maroon:"#800000",mediumaquamarine:"#66cdaa",mediumblue:"#0000cd",mediumorchid:"#ba55d3",mediumpurple:"#9370d8",mediumseagreen:"#3cb371",mediumslateblue:"#7b68ee",mediumspringgreen:"#00fa9a",mediumturquoise:"#48d1cc",mediumvioletred:"#c71585",midnightblue:"#191970",mintcream:"#f5fffa",mistyrose:"#ffe4e1",moccasin:"#ffe4b5",navajowhite:"#ffdead",navy:"#000080",oldlace:"#fdf5e6",olive:"#808000",olivedrab:"#6b8e23",orange:"#ffa500",orangered:"#ff4500",orchid:"#da70d6",palegoldenrod:"#eee8aa",palegreen:"#98fb98",paleturquoise:"#afeeee",palevioletred:"#d87093",papayawhip:"#ffefd5",peachpuff:"#ffdab9",peru:"#cd853f",pink:"#ffc0cb",plum:"#dda0dd",powderblue:"#b0e0e6",purple:"#800080",red:"#ff0000",rosybrown:"#bc8f8f",royalblue:"#4169e1",saddlebrown:"#8b4513",salmon:"#fa8072",sandybrown:"#f4a460",seagreen:"#2e8b57",seashell:"#fff5ee",sienna:"#a0522d",silver:"#c0c0c0",skyblue:"#87ceeb",slateblue:"#6a5acd",slategray:"#708090",snow:"#fffafa",springgreen:"#00ff7f",steelblue:"#4682b4",tan:"#d2b48c",teal:"#008080",thistle:"#d8bfd8",tomato:"#ff6347",turquoise:"#40e0d0",violet:"#ee82ee",wheat:"#f5deb3",white:"#ffffff",whitesmoke:"#f5f5f5",yellow:"#ffff00",yellowgreen:"#9acd32"},_sanitizeNumber:function(a){return"number"==typeof a?a:isNaN(a)||null===a||""===a||void 0===a?1:void 0!==a.toLowerCase?parseFloat(a):1},setColor:function(a){a=a.toLowerCase(),this.value=this.stringToHSB(a)||{h:0,s:0,b:0,a:1}},stringToHSB:function(b){b=b.toLowerCase();var c=this,d=!1;return a.each(this.stringParsers,function(a,e){var f=e.re.exec(b),g=f&&e.parse.apply(c,[f]),h=e.format||"rgba";return g?(d=h.match(/hsla?/)?c.RGBtoHSB.apply(c,c.HSLtoRGB.apply(c,g)):c.RGBtoHSB.apply(c,g),c.origFormat=h,!1):!0}),d},setHue:function(a){this.value.h=1-a},setSaturation:function(a){this.value.s=a},setBrightness:function(a){this.value.b=1-a},setAlpha:function(a){this.value.a=parseInt(100*(1-a),10)/100},toRGB:function(a,b,c,d){a||(a=this.value.h,b=this.value.s,c=this.value.b),a*=360;var e,f,g,h,i;return a=a%360/60,i=c*b,h=i*(1-Math.abs(a%2-1)),e=f=g=c-i,a=~~a,e+=[i,h,0,0,h,i][a],f+=[h,i,i,h,0,0][a],g+=[0,0,h,i,i,h][a],{r:Math.round(255*e),g:Math.round(255*f),b:Math.round(255*g),a:d||this.value.a}},toHex:function(a,b,c,d){var e=this.toRGB(a,b,c,d);return"#"+(1<<24|parseInt(e.r)<<16|parseInt(e.g)<<8|parseInt(e.b)).toString(16).substr(1)},toHSL:function(a,b,c,d){a=a||this.value.h,b=b||this.value.s,c=c||this.value.b,d=d||this.value.a;var e=a,f=(2-b)*c,g=b*c;return g/=f>0&&1>=f?f:2-f,f/=2,g>1&&(g=1),{h:isNaN(e)?0:e,s:isNaN(g)?0:g,l:isNaN(f)?0:f,a:isNaN(d)?0:d}},toAlias:function(a,b,c,d){var e=this.toHex(a,b,c,d);for(var f in this.colors)if(this.colors[f]==e)return f;return!1},RGBtoHSB:function(a,b,c,d){a/=255,b/=255,c/=255;var e,f,g,h;return g=Math.max(a,b,c),h=g-Math.min(a,b,c),e=0===h?null:g===a?(b-c)/h:g===b?(c-a)/h+2:(a-b)/h+4,e=(e+360)%6*60/360,f=0===h?0:h/g,{h:this._sanitizeNumber(e),s:f,b:g,a:this._sanitizeNumber(d)}},HueToRGB:function(a,b,c){return 0>c?c+=1:c>1&&(c-=1),1>6*c?a+(b-a)*c*6:1>2*c?b:2>3*c?a+(b-a)*(2/3-c)*6:a},HSLtoRGB:function(a,b,c,d){0>b&&(b=0);var e;e=.5>=c?c*(1+b):c+b-c*b;var f=2*c-e,g=a+1/3,h=a,i=a-1/3,j=Math.round(255*this.HueToRGB(f,e,g)),k=Math.round(255*this.HueToRGB(f,e,h)),l=Math.round(255*this.HueToRGB(f,e,i));return[j,k,l,this._sanitizeNumber(d)]},toString:function(a){switch(a=a||"rgba"){case"rgb":var b=this.toRGB();return"rgb("+b.r+","+b.g+","+b.b+")";case"rgba":var b=this.toRGB();return"rgba("+b.r+","+b.g+","+b.b+","+b.a+")";case"hsl":var c=this.toHSL();return"hsl("+Math.round(360*c.h)+","+Math.round(100*c.s)+"%,"+Math.round(100*c.l)+"%)";case"hsla":var c=this.toHSL();return"hsla("+Math.round(360*c.h)+","+Math.round(100*c.s)+"%,"+Math.round(100*c.l)+"%,"+c.a+")";case"hex":return this.toHex();case"alias":return this.toAlias()||this.toHex();default:return!1}},stringParsers:[{re:/#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})/,format:"hex",parse:function(a){return[parseInt(a[1],16),parseInt(a[2],16),parseInt(a[3],16),1]}},{re:/#([a-fA-F0-9])([a-fA-F0-9])([a-fA-F0-9])/,format:"hex",parse:function(a){return[parseInt(a[1]+a[1],16),parseInt(a[2]+a[2],16),parseInt(a[3]+a[3],16),1]}},{re:/rgb\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*?\)/,format:"rgb",parse:function(a){return[a[1],a[2],a[3],1]}},{re:/rgb\(\s*(\d+(?:\.\d+)?)\%\s*,\s*(\d+(?:\.\d+)?)\%\s*,\s*(\d+(?:\.\d+)?)\%\s*?\)/,format:"rgb",parse:function(a){return[2.55*a[1],2.55*a[2],2.55*a[3],1]}},{re:/rgba\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*(?:,\s*(\d+(?:\.\d+)?)\s*)?\)/,format:"rgba",parse:function(a){return[a[1],a[2],a[3],a[4]]}},{re:/rgba\(\s*(\d+(?:\.\d+)?)\%\s*,\s*(\d+(?:\.\d+)?)\%\s*,\s*(\d+(?:\.\d+)?)\%\s*(?:,\s*(\d+(?:\.\d+)?)\s*)?\)/,format:"rgba",parse:function(a){return[2.55*a[1],2.55*a[2],2.55*a[3],a[4]]}},{re:/hsl\(\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)\%\s*,\s*(\d+(?:\.\d+)?)\%\s*?\)/,format:"hsl",parse:function(a){return[a[1]/360,a[2]/100,a[3]/100,a[4]]}},{re:/hsla\(\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)\%\s*,\s*(\d+(?:\.\d+)?)\%\s*(?:,\s*(\d+(?:\.\d+)?)\s*)?\)/,format:"hsla",parse:function(a){return[a[1]/360,a[2]/100,a[3]/100,a[4]]}},{re:/^([a-z]{3,})$/,format:"alias",parse:function(a){var b=this.colorNameToHex(a[0])||"#000000",c=this.stringParsers[0].re.exec(b),d=c&&this.stringParsers[0].parse.apply(this,[c]);return d}}],colorNameToHex:function(a){return"undefined"!=typeof this.colors[a.toLowerCase()]?this.colors[a.toLowerCase()]:!1}};var c={horizontal:!1,inline:!1,color:!1,format:!1,input:"input",container:!1,component:".add-on, .input-group-addon",sliders:{saturation:{maxLeft:100,maxTop:100,callLeft:"setSaturation",callTop:"setBrightness"},hue:{maxLeft:0,maxTop:100,callLeft:!1,callTop:"setHue"},alpha:{maxLeft:0,maxTop:100,callLeft:!1,callTop:"setAlpha"}},slidersHorz:{saturation:{maxLeft:100,maxTop:100,callLeft:"setSaturation",callTop:"setBrightness"},hue:{maxLeft:100,maxTop:0,callLeft:"setHue",callTop:!1},alpha:{maxLeft:100,maxTop:0,callLeft:"setAlpha",callTop:!1}},template:'<div class="colorpicker dropdown-menu"><div class="colorpicker-saturation"><i><b></b></i></div><div class="colorpicker-hue"><i></i></div><div class="colorpicker-alpha"><i></i></div><div class="colorpicker-color"><div /></div></div>'},d=function(d,e){this.element=a(d).addClass("colorpicker-element"),this.options=a.extend({},c,this.element.data(),e),this.component=this.options.component,this.component=this.component!==!1?this.element.find(this.component):!1,this.component&&0===this.component.length&&(this.component=!1),this.container=this.options.container===!0?this.element:this.options.container,this.container=this.container!==!1?a(this.container):!1,this.input=this.element.is("input")?this.element:this.options.input?this.element.find(this.options.input):!1,this.input&&0===this.input.length&&(this.input=!1),this.color=new b(this.options.color!==!1?this.options.color:this.getValue()),this.format=this.options.format!==!1?this.options.format:this.color.origFormat,this.picker=a(this.options.template),this.picker.addClass(this.options.inline?"colorpicker-inline colorpicker-visible":"colorpicker-hidden"),this.options.horizontal&&this.picker.addClass("colorpicker-horizontal"),("rgba"===this.format||"hsla"===this.format)&&this.picker.addClass("colorpicker-with-alpha"),this.picker.on("mousedown.colorpicker",a.proxy(this.mousedown,this)),this.picker.appendTo(this.container?this.container:a("body")),this.input!==!1&&(this.input.on({"keyup.colorpicker":a.proxy(this.keyup,this)}),this.component===!1&&this.element.on({"focus.colorpicker":a.proxy(this.show,this)}),this.options.inline===!1&&this.element.on({"focusout.colorpicker":a.proxy(this.hide,this)})),this.component!==!1&&this.component.on({"click.colorpicker":a.proxy(this.show,this)}),this.input===!1&&this.component===!1&&this.element.on({"click.colorpicker":a.proxy(this.show,this)}),this.update(),a(a.proxy(function(){this.element.trigger("create")},this))};d.version="2.0.0-beta",d.Color=b,d.prototype={constructor:d,destroy:function(){this.picker.remove(),this.element.removeData("colorpicker").off(".colorpicker"),this.input!==!1&&this.input.off(".colorpicker"),this.component!==!1&&this.component.off(".colorpicker"),this.element.removeClass("colorpicker-element"),this.element.trigger({type:"destroy"})},reposition:function(){if(this.options.inline!==!1)return!1;var a=this.container&&this.container[0]!==document.body?"position":"offset",b=this.component?this.component[a]():this.element[a]();this.picker.css({top:b.top+(this.component?this.component.outerHeight():this.element.outerHeight()),left:b.left})},show:function(b){return this.isDisabled()?!1:(this.picker.addClass("colorpicker-visible").removeClass("colorpicker-hidden"),this.reposition(),a(window).on("resize.colorpicker",a.proxy(this.reposition,this)),!this.hasInput()&&b&&b.stopPropagation&&b.preventDefault&&(b.stopPropagation(),b.preventDefault()),this.options.inline===!1&&a(window.document).on({"mousedown.colorpicker":a.proxy(this.hide,this)}),void this.element.trigger({type:"showPicker",color:this.color}))},hide:function(){this.picker.addClass("colorpicker-hidden").removeClass("colorpicker-visible"),a(window).off("resize.colorpicker",this.reposition),a(document).off({"mousedown.colorpicker":this.hide}),this.update(),this.element.trigger({type:"hidePicker",color:this.color})},updateData:function(a){return a=a||this.color.toString(this.format),this.element.data("color",a),a},updateInput:function(a){return a=a||this.color.toString(this.format),this.input!==!1&&this.input.prop("value",a),a},updatePicker:function(a){void 0!==a&&(this.color=new b(a));var c=this.options.horizontal===!1?this.options.sliders:this.options.slidersHorz,d=this.picker.find("i");return 0!==d.length?(this.options.horizontal===!1?(c=this.options.sliders,d.eq(1).css("top",c.hue.maxTop*(1-this.color.value.h)).end().eq(2).css("top",c.alpha.maxTop*(1-this.color.value.a))):(c=this.options.slidersHorz,d.eq(1).css("left",c.hue.maxLeft*(1-this.color.value.h)).end().eq(2).css("left",c.alpha.maxLeft*(1-this.color.value.a))),d.eq(0).css({top:c.saturation.maxTop-this.color.value.b*c.saturation.maxTop,left:this.color.value.s*c.saturation.maxLeft}),this.picker.find(".colorpicker-saturation").css("backgroundColor",this.color.toHex(this.color.value.h,1,1,1)),this.picker.find(".colorpicker-alpha").css("backgroundColor",this.color.toHex()),this.picker.find(".colorpicker-color, .colorpicker-color div").css("backgroundColor",this.color.toString(this.format)),a):void 0},updateComponent:function(a){if(a=a||this.color.toString(this.format),this.component!==!1){var b=this.component.find("i").eq(0);b.length>0?b.css({backgroundColor:a}):this.component.css({backgroundColor:a})}return a},update:function(a){var b=this.updateComponent();return(this.getValue(!1)!==!1||a===!0)&&(this.updateInput(b),this.updateData(b)),this.updatePicker(),b},setValue:function(a){this.color=new b(a),this.update(),this.element.trigger({type:"changeColor",color:this.color,value:a})},getValue:function(a){a=void 0===a?"#000000":a;var b;return b=this.hasInput()?this.input.val():this.element.data("color"),(void 0===b||""===b||null===b)&&(b=a),b},hasInput:function(){return this.input!==!1},isDisabled:function(){return this.hasInput()?this.input.prop("disabled")===!0:!1},disable:function(){return this.hasInput()?(this.input.prop("disabled",!0),!0):!1},enable:function(){return this.hasInput()?(this.input.prop("disabled",!1),!0):!1},currentSlider:null,mousePointer:{left:0,top:0},mousedown:function(b){b.stopPropagation(),b.preventDefault();var c=a(b.target),d=c.closest("div"),e=this.options.horizontal?this.options.slidersHorz:this.options.sliders;if(!d.is(".colorpicker")){if(d.is(".colorpicker-saturation"))this.currentSlider=a.extend({},e.saturation);else if(d.is(".colorpicker-hue"))this.currentSlider=a.extend({},e.hue);else{if(!d.is(".colorpicker-alpha"))return!1;this.currentSlider=a.extend({},e.alpha)}var f=d.offset();this.currentSlider.guide=d.find("i")[0].style,this.currentSlider.left=b.pageX-f.left,this.currentSlider.top=b.pageY-f.top,this.mousePointer={left:b.pageX,top:b.pageY},a(document).on({"mousemove.colorpicker":a.proxy(this.mousemove,this),"mouseup.colorpicker":a.proxy(this.mouseup,this)}).trigger("mousemove")}return!1},mousemove:function(a){a.stopPropagation(),a.preventDefault();var b=Math.max(0,Math.min(this.currentSlider.maxLeft,this.currentSlider.left+((a.pageX||this.mousePointer.left)-this.mousePointer.left))),c=Math.max(0,Math.min(this.currentSlider.maxTop,this.currentSlider.top+((a.pageY||this.mousePointer.top)-this.mousePointer.top)));return this.currentSlider.guide.left=b+"px",this.currentSlider.guide.top=c+"px",this.currentSlider.callLeft&&this.color[this.currentSlider.callLeft].call(this.color,b/100),this.currentSlider.callTop&&this.color[this.currentSlider.callTop].call(this.color,c/100),this.update(!0),this.element.trigger({type:"changeColor",color:this.color}),!1},mouseup:function(b){return b.stopPropagation(),b.preventDefault(),a(document).off({"mousemove.colorpicker":this.mousemove,"mouseup.colorpicker":this.mouseup}),!1},keyup:function(a){if(38===a.keyCode)this.color.value.a<1&&(this.color.value.a=Math.round(100*(this.color.value.a+.01))/100),this.update(!0);else if(40===a.keyCode)this.color.value.a>0&&(this.color.value.a=Math.round(100*(this.color.value.a-.01))/100),this.update(!0);else{var c=this.input.val();this.color=new b(c),this.getValue(!1)!==!1&&(this.updateData(),this.updateComponent(),this.updatePicker())}this.element.trigger({type:"changeColor",color:this.color,value:c})}},a.colorpicker=d,a.fn.colorpicker=function(b){var c=arguments;return this.each(function(){var e=a(this),f=e.data("colorpicker"),g="object"==typeof b?b:{};f||"string"==typeof b?"string"==typeof b&&f[b].apply(f,Array.prototype.slice.call(c,1)):e.data("colorpicker",new d(this,g))})},a.fn.colorpicker.constructor=d});

/** Appear
	https://github.com/bas2k/jquery.appear/
 **************************************************************** **/
(function(a){a.fn.appear=function(d,b){var c=a.extend({data:undefined,one:true,accX:0,accY:0},b);return this.each(function(){var g=a(this);g.appeared=false;if(!d){g.trigger("appear",c.data);return}var f=a(window);var e=function(){if(!g.is(":visible")){g.appeared=false;return}var r=f.scrollLeft();var q=f.scrollTop();var l=g.offset();var s=l.left;var p=l.top;var i=c.accX;var t=c.accY;var k=g.height();var j=f.height();var n=g.width();var m=f.width();if(p+k+t>=q&&p<=q+j+t&&s+n+i>=r&&s<=r+m+i){if(!g.appeared){g.trigger("appear",c.data)}}else{g.appeared=false}};var h=function(){g.appeared=true;if(c.one){f.unbind("scroll",e);var j=a.inArray(e,a.fn.appear.checks);if(j>=0){a.fn.appear.checks.splice(j,1)}}d.apply(this,arguments)};if(c.one){g.one("appear",c.data,h)}else{g.bind("appear",c.data,h)}f.scroll(e);a.fn.appear.checks.push(e);(e)()})};a.extend(a.fn.appear,{checks:[],timeout:null,checkAll:function(){var b=a.fn.appear.checks.length;if(b>0){while(b--){(a.fn.appear.checks[b])()}}},run:function(){if(a.fn.appear.timeout){clearTimeout(a.fn.appear.timeout)}a.fn.appear.timeout=setTimeout(a.fn.appear.checkAll,20)}});a.each(["append","prepend","after","before","attr","removeAttr","addClass","removeClass","toggleClass","remove","css","show","hide"],function(c,d){var b=a.fn[d];if(b){a.fn[d]=function(){var e=b.apply(this,arguments);a.fn.appear.run();return e}}})})(jQuery);


/** Parallax
	http://www.ianlunn.co.uk/plugins/jquery-parallax/
 **************************************************************** **/
(function(a){var b=a(window);var c=b.height();b.resize(function(){c=b.height()});a.fn.parallax=function(e,d,g){var i=a(this);var j;var h;var f=0;function k(){i.each(function(){h=i.offset().top});if(g){j=function(m){return m.outerHeight(true)}}else{j=function(m){return m.height()}}if(arguments.length<1||e===null){e="50%"}if(arguments.length<2||d===null){d=0.5}if(arguments.length<3||g===null){g=true}var l=b.scrollTop();i.each(function(){var n=a(this);var o=n.offset().top;var m=j(n);if(o+m<l||o>l+c){return}i.css("backgroundPosition",e+" "+Math.round((h-l)*d)+"px")})}b.bind("scroll",k).resize(k);k()}})(jQuery);


/** jQuery Easing v1.3
	http://gsgd.co.uk/sandbox/jquery/easing/
 **************************************************************** **/
jQuery.easing.jswing=jQuery.easing.swing;jQuery.extend(jQuery.easing,{def:"easeOutQuad",swing:function(e,f,a,h,g){return jQuery.easing[jQuery.easing.def](e,f,a,h,g)},easeInQuad:function(e,f,a,h,g){return h*(f/=g)*f+a},easeOutQuad:function(e,f,a,h,g){return -h*(f/=g)*(f-2)+a},easeInOutQuad:function(e,f,a,h,g){if((f/=g/2)<1){return h/2*f*f+a}return -h/2*((--f)*(f-2)-1)+a},easeInCubic:function(e,f,a,h,g){return h*(f/=g)*f*f+a},easeOutCubic:function(e,f,a,h,g){return h*((f=f/g-1)*f*f+1)+a},easeInOutCubic:function(e,f,a,h,g){if((f/=g/2)<1){return h/2*f*f*f+a}return h/2*((f-=2)*f*f+2)+a},easeInQuart:function(e,f,a,h,g){return h*(f/=g)*f*f*f+a},easeOutQuart:function(e,f,a,h,g){return -h*((f=f/g-1)*f*f*f-1)+a},easeInOutQuart:function(e,f,a,h,g){if((f/=g/2)<1){return h/2*f*f*f*f+a}return -h/2*((f-=2)*f*f*f-2)+a},easeInQuint:function(e,f,a,h,g){return h*(f/=g)*f*f*f*f+a},easeOutQuint:function(e,f,a,h,g){return h*((f=f/g-1)*f*f*f*f+1)+a},easeInOutQuint:function(e,f,a,h,g){if((f/=g/2)<1){return h/2*f*f*f*f*f+a}return h/2*((f-=2)*f*f*f*f+2)+a},easeInSine:function(e,f,a,h,g){return -h*Math.cos(f/g*(Math.PI/2))+h+a},easeOutSine:function(e,f,a,h,g){return h*Math.sin(f/g*(Math.PI/2))+a},easeInOutSine:function(e,f,a,h,g){return -h/2*(Math.cos(Math.PI*f/g)-1)+a},easeInExpo:function(e,f,a,h,g){return(f==0)?a:h*Math.pow(2,10*(f/g-1))+a},easeOutExpo:function(e,f,a,h,g){return(f==g)?a+h:h*(-Math.pow(2,-10*f/g)+1)+a},easeInOutExpo:function(e,f,a,h,g){if(f==0){return a}if(f==g){return a+h}if((f/=g/2)<1){return h/2*Math.pow(2,10*(f-1))+a}return h/2*(-Math.pow(2,-10*--f)+2)+a},easeInCirc:function(e,f,a,h,g){return -h*(Math.sqrt(1-(f/=g)*f)-1)+a},easeOutCirc:function(e,f,a,h,g){return h*Math.sqrt(1-(f=f/g-1)*f)+a},easeInOutCirc:function(e,f,a,h,g){if((f/=g/2)<1){return -h/2*(Math.sqrt(1-f*f)-1)+a}return h/2*(Math.sqrt(1-(f-=2)*f)+1)+a},easeInElastic:function(f,h,e,l,k){var i=1.70158;var j=0;var g=l;if(h==0){return e}if((h/=k)==1){return e+l}if(!j){j=k*0.3}if(g<Math.abs(l)){g=l;var i=j/4}else{var i=j/(2*Math.PI)*Math.asin(l/g)}return -(g*Math.pow(2,10*(h-=1))*Math.sin((h*k-i)*(2*Math.PI)/j))+e},easeOutElastic:function(f,h,e,l,k){var i=1.70158;var j=0;var g=l;if(h==0){return e}if((h/=k)==1){return e+l}if(!j){j=k*0.3}if(g<Math.abs(l)){g=l;var i=j/4}else{var i=j/(2*Math.PI)*Math.asin(l/g)}return g*Math.pow(2,-10*h)*Math.sin((h*k-i)*(2*Math.PI)/j)+l+e},easeInOutElastic:function(f,h,e,l,k){var i=1.70158;var j=0;var g=l;if(h==0){return e}if((h/=k/2)==2){return e+l}if(!j){j=k*(0.3*1.5)}if(g<Math.abs(l)){g=l;var i=j/4}else{var i=j/(2*Math.PI)*Math.asin(l/g)}if(h<1){return -0.5*(g*Math.pow(2,10*(h-=1))*Math.sin((h*k-i)*(2*Math.PI)/j))+e}return g*Math.pow(2,-10*(h-=1))*Math.sin((h*k-i)*(2*Math.PI)/j)*0.5+l+e},easeInBack:function(e,f,a,i,h,g){if(g==undefined){g=1.70158}return i*(f/=h)*f*((g+1)*f-g)+a},easeOutBack:function(e,f,a,i,h,g){if(g==undefined){g=1.70158}return i*((f=f/h-1)*f*((g+1)*f+g)+1)+a},easeInOutBack:function(e,f,a,i,h,g){if(g==undefined){g=1.70158}if((f/=h/2)<1){return i/2*(f*f*(((g*=(1.525))+1)*f-g))+a}return i/2*((f-=2)*f*(((g*=(1.525))+1)*f+g)+2)+a},easeInBounce:function(e,f,a,h,g){return h-jQuery.easing.easeOutBounce(e,g-f,0,h,g)+a},easeOutBounce:function(e,f,a,h,g){if((f/=g)<(1/2.75)){return h*(7.5625*f*f)+a}else{if(f<(2/2.75)){return h*(7.5625*(f-=(1.5/2.75))*f+0.75)+a}else{if(f<(2.5/2.75)){return h*(7.5625*(f-=(2.25/2.75))*f+0.9375)+a}else{return h*(7.5625*(f-=(2.625/2.75))*f+0.984375)+a}}}},easeInOutBounce:function(e,f,a,h,g){if(f<g/2){return jQuery.easing.easeInBounce(e,f*2,0,h,g)*0.5+a}return jQuery.easing.easeOutBounce(e,f*2-g,0,h,g)*0.5+h*0.5+a}});

/** Backstretch v2.0.4
	http://srobbin.com/jquery-plugins/backstretch/
 **************************************************************** **/
(function(a,d,p){a.fn.backstretch=function(c,b){(c===p||0===c.length)&&a.error("No images were supplied for Backstretch");0===a(d).scrollTop()&&d.scrollTo(0,0);return this.each(function(){var d=a(this),g=d.data("backstretch");if(g){if("string"==typeof c&&"function"==typeof g[c]){g[c](b);return}b=a.extend(g.options,b);g.destroy(!0)}g=new q(this,c,b);d.data("backstretch",g)})};a.backstretch=function(c,b){return a("body").backstretch(c,b).data("backstretch")};a.expr[":"].backstretch=function(c){return a(c).data("backstretch")!==p};a.fn.backstretch.defaults={centeredX:!0,centeredY:!0,duration:5E3,fade:0};var r={left:0,top:0,overflow:"hidden",margin:0,padding:0,height:"100%",width:"100%",zIndex:-999999},s={position:"absolute",display:"none",margin:0,padding:0,border:"none",width:"auto",height:"auto",maxHeight:"none",maxWidth:"none",zIndex:-999999},q=function(c,b,e){this.options=a.extend({},a.fn.backstretch.defaults,e||{});this.images=a.isArray(b)?b:[b];a.each(this.images,function(){a("<img />")[0].src=this});this.isBody=c===document.body;this.$container=a(c);this.$root=this.isBody?l?a(d):a(document):this.$container;c=this.$container.children(".backstretch").first();this.$wrap=c.length?c:a('<div class="backstretch"></div>').css(r).appendTo(this.$container);this.isBody||(c=this.$container.css("position"),b=this.$container.css("zIndex"),this.$container.css({position:"static"===c?"relative":c,zIndex:"auto"===b?0:b,background:"none"}),this.$wrap.css({zIndex:-999998}));this.$wrap.css({position:this.isBody&&l?"fixed":"absolute"});this.index=0;this.show(this.index);a(d).on("resize.backstretch",a.proxy(this.resize,this)).on("orientationchange.backstretch",a.proxy(function(){this.isBody&&0===d.pageYOffset&&(d.scrollTo(0,1),this.resize())},this))};q.prototype={resize:function(){try{var a={left:0,top:0},b=this.isBody?this.$root.width():this.$root.innerWidth(),e=b,g=this.isBody?d.innerHeight?d.innerHeight:this.$root.height():this.$root.innerHeight(),j=e/this.$img.data("ratio"),f;j>=g?(f=(j-g)/2,this.options.centeredY&&(a.top="-"+f+"px")):(j=g,e=j*this.$img.data("ratio"),f=(e-b)/2,this.options.centeredX&&(a.left="-"+f+"px"));this.$wrap.css({width:b,height:g}).find("img:not(.deleteable)").css({width:e,height:j}).css(a)}catch(h){}return this},show:function(c){if(!(Math.abs(c)>this.images.length-1)){var b=this,e=b.$wrap.find("img").addClass("deleteable"),d={relatedTarget:b.$container[0]};b.$container.trigger(a.Event("backstretch.before",d),[b,c]);this.index=c;clearInterval(b.interval);b.$img=a("<img />").css(s).bind("load",function(f){var h=this.width||a(f.target).width();f=this.height||a(f.target).height();a(this).data("ratio",h/f);a(this).fadeIn(b.options.speed||b.options.fade,function(){e.remove();b.paused||b.cycle();a(["after","show"]).each(function(){b.$container.trigger(a.Event("backstretch."+this,d),[b,c])})});b.resize()}).appendTo(b.$wrap);b.$img.attr("src",b.images[c]);return b}},next:function(){return this.show(this.index<this.images.length-1?this.index+1:0)},prev:function(){return this.show(0===this.index?this.images.length-1:this.index-1)},pause:function(){this.paused=!0;return this},resume:function(){this.paused=!1;this.next();return this},cycle:function(){1<this.images.length&&(clearInterval(this.interval),this.interval=setInterval(a.proxy(function(){this.paused||this.next()},this),this.options.duration));return this},destroy:function(c){a(d).off("resize.backstretch orientationchange.backstretch");clearInterval(this.interval);c||this.$wrap.remove();this.$container.removeData("backstretch")}};var l,f=navigator.userAgent,m=navigator.platform,e=f.match(/AppleWebKit\/([0-9]+)/),e=!!e&&e[1],h=f.match(/Fennec\/([0-9]+)/),h=!!h&&h[1],n=f.match(/Opera Mobi\/([0-9]+)/),t=!!n&&n[1],k=f.match(/MSIE ([0-9]+)/),k=!!k&&k[1];l=!((-1<m.indexOf("iPhone")||-1<m.indexOf("iPad")||-1<m.indexOf("iPod"))&&e&&534>e||d.operamini&&"[object OperaMini]"==={}.toString.call(d.operamini)||n&&7458>t||-1<f.indexOf("Android")&&e&&533>e||h&&6>h||"palmGetResource"in d&&e&&534>e||-1<f.indexOf("MeeGo")&&-1<f.indexOf("NokiaBrowser/8.5.0")||k&&6>=k)})(jQuery,window);