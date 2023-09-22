	// @CONTACT FORM - TRANSLATE OR EDIT
	errMsg 					= 'Please complete all fields!',
	errEmail				= 'Invalid Email!',
	okSent					= '<strong>Thank You!</strong> Your message successfully sent!',
	buttonDisabled			= 'MESSAGE SENT';


/**	CONTACT FORM
*************************************************** **/
	jQuery("#contact_submit").bind("click", function(e) {
		e.preventDefault();

		var contact_name 	= jQuery("#contact_name").val(),			// required
			contact_email 	= jQuery("#contact_email").val(),			// required
			contact_subject = jQuery("#contact_subject").val(),			// optional
			contact_comment = jQuery("#contact_comment").val(),			// required
			captcha 		= jQuery("#captcha").val(),					// required TO BE EMPTY for humans
			_action			= jQuery("#contactForm").attr('action'),	// form action URL
			_method			= jQuery("#contactForm").attr('method'),	// form method
			_err			= false;									// status

		// Remove error class
		jQuery("input, textarea").removeClass('has-error');

		// Spam bots will see captcha field - that's how we detect spams.
		// It's very simple and not very efficient antispam method but works for bots.
		if(captcha != '') {
			return false;
		}


		// Name Check
		if(contact_name == '') {
			jQuery("#contact_name").addClass('has-error');
			var _err = true;
		}

		// Email Check
		if(contact_email == '') {
			jQuery("#contact_email").addClass('has-error');
			var _err = true;
		}

		// Comment Check
		if(contact_comment == '') {
			jQuery("#contact_comment").addClass('has-error');
			var _err = true;
		}

		// Stop here, we have empty fields!
		if(_err === true) {
			return false;
		}


		// SEND MAIL VIA AJAX
		$.ajax({
			url: 	_action,
			data: 	{ajax:"true", action:'email_send', contact_name:contact_name, contact_email:contact_email, contact_comment:contact_comment, contact_subject:contact_subject},
			type: 	_method,
			error: 	function(XMLHttpRequest, textStatus, errorThrown) {

				alert(errorThrown); // usualy on headers 404 or Internal Server Error

			},

			success: function(data) {
				data = data.trim(); // remove output spaces


				// PHP RETURN: Mandatory Fields
				if(data == '_required_') {
					alert(errMsg);
				} else

				// PHP RETURN: INVALID EMAIL
				if(data == '_invalid_email_') {
					alert(errEmail);
				} else

				// VALID EMAIL
				if(data == '_sent_ok_') {

					// append message and show ok alert
					jQuery("#_msg_txt_").empty().append(okSent);
					jQuery("#_sent_ok_").removeClass('hide');

					// reset form
					jQuery("#contact_name, #contact_email, #contact_subject, #contact_comment").val('');

					// disable button - message already sent!
					jQuery("#contact_submit").empty().append(buttonDisabled);
					jQuery("#contact_submit").addClass('disabled');

				} else {

					// PHPMAILER ERROR
					alert(data); 

				}
			}
		});

	});
