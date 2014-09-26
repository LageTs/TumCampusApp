package de.tum.in.tumcampus.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.ActivityForAccessingTumOnline;
import de.tum.in.tumcampus.adapters.OrgDetailsItemHandler;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.OrgDetailsItem;
import de.tum.in.tumcampus.tumonline.TUMOnlineConst;

/**
 * Show all details that are available on TUMCampus to any organisation
 */
@SuppressLint("DefaultLocale")
public class OrganisationDetailsActivity extends ActivityForAccessingTumOnline {

	/**
	 * Id of the organisation of which the details should be shown
	 */
	private String orgId;

	/**
	 * Only for setting it in the caption at the top
	 */
	private String orgName;

	public OrganisationDetailsActivity() {
		super(TUMOnlineConst.ORG_DETAILS, R.layout.activity_organisationdetails);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the submitted (bundle) data
        Bundle bundle = this.getIntent().getExtras();
        orgId = bundle.getString(Const.ORG_ID);
        orgName = bundle.getString(Const.ORG_NAME);
    }

    @Override
    public void onStart() {
        super.onStart();
        // if there is a call of OrganisationDetails without an id (should not
        // be possible)
        if (orgId == null) {
            Utils.showToast(this, R.string.invalid_organisation);
            return;
        }

        // set the name of the organisation as heading (TextView tvCaption)
        // only load the details if the details page is new and it isn't a
        // return from a link
        TextView tvCaption = (TextView) findViewById(R.id.tvCaption);
        if (tvCaption.getText().toString().compareTo(orgName) != 0) {

            // set the new organisation name in the heading
            tvCaption.setText(orgName.toUpperCase());

            // Initialise the request handler and append the orgUnitID to the URL
            requestHandler.setParameter("pOrgNr", orgId);
            super.requestFetch();
        }
    }

	/**
	 * Initialize BackButton -> On Click: Go to Organisation.java and show the
	 * Organisation Tree
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public void onBackPressed() {

		// if button "back" is clicked -> make a new Bundle with the orgId and
		// start Organisation-Activity
		Intent intent = new Intent(OrganisationDetailsActivity.this,
				OrganisationActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		Bundle bundle = new Bundle();
		bundle.putString(Const.ORG_ID, orgId);
		intent.putExtras(bundle);

		startActivity(intent);
	}

	/**
	 * When the data has arrived call this function, parse the Data and Update
	 * the UserInterface
	 *
	 * @param rawResponse XML-TUMCampus-Response (String)
	 */
	@Override
	public void onFetch(String rawResponse) {
		// parse XML into one OrgDetailsItem
		OrgDetailsItem orgDetailsItem = parseOrgDetails(rawResponse);
		updateUI(orgDetailsItem);
		showLoadingEnded();
	}

    /**
     * Parse XML-String into one OrgDetails-Object
     *
     * @param rawResp XML-String to parse
     * @return OrgDetailsItem (OrgDetails Object)
     */
    private static OrgDetailsItem parseOrgDetails(String rawResp) {

		/* Get a SAXParser from the SAXPArserFactory. */
        SAXParserFactory sxParserFactory = SAXParserFactory.newInstance();
        SAXParser sxParser;
        try {
            sxParser = sxParserFactory.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
            XMLReader xmlReader = sxParser.getXMLReader();
			/* Create a new ContentHandler and apply it to the XML-Reader */
            OrgDetailsItemHandler orgDetailsItem = new OrgDetailsItemHandler();
            xmlReader.setContentHandler(orgDetailsItem);

			/* Parse the xml-data from our URL. */
            xmlReader.parse(new InputSource(new StringReader(rawResp)));

            return orgDetailsItem.getDetails();

        } catch (Exception e) {
            Utils.log(e);
        }
		/* Parsing has finished. */
        return null;
    }

	/**
	 * Show the Organisation Details to the user
	 * 
	 * @param organisation organisation detail object
	 */
	private void updateUI(OrgDetailsItem organisation) {
		// catch error
		if (organisation == null) {
			return;
		}

		TextView identifier = (TextView) findViewById(R.id.identifier);
		TextView name = (TextView) findViewById(R.id.name);
		TextView contact = (TextView) findViewById(R.id.contact);
		TextView address = (TextView) findViewById(R.id.adress);
		TextView homepage = (TextView) findViewById(R.id.homepage);
		TextView email = (TextView) findViewById(R.id.email);
		TextView phone = (TextView) findViewById(R.id.phone);
		TextView fax = (TextView) findViewById(R.id.fax);
		TextView secretary = (TextView) findViewById(R.id.secretary);
		TextView extraCaption = (TextView) findViewById(R.id.extra_name);
		TextView extra = (TextView) findViewById(R.id.extra);
		TextView bib = (TextView) findViewById(R.id.bib);

		identifier.setText(organisation.getCode());
		name.setText(organisation.getName());
		contact.setText(organisation.getContactName());
		address.setText(organisation.getContactStreet());
		homepage.setText(organisation.getContactLocationURL());
		email.setText(organisation.getContactEmail());
		phone.setText(organisation.getContactTelephone());
		fax.setText(organisation.getContactFax());
		secretary.setText(organisation.getContactLocality());
		extraCaption.setText(organisation.getAdditionalInfoCaption());
		extra.setText(organisation.getAdditionalInfoText());
		bib.setText(organisation.getContactLocality());

		if (identifier.getText().length() == 0) {
			((View) identifier.getParent()).setVisibility(View.GONE);
		}
		if (name.getText().length() == 0) {
			((View) name.getParent()).setVisibility(View.GONE);
		}
		if (contact.getText().length() == 0) {
			((View) contact.getParent()).setVisibility(View.GONE);
		}
		if (address.getText().length() == 0) {
			((View) address.getParent()).setVisibility(View.GONE);
		}
		if (homepage.getText().length() == 0) {
			((View) homepage.getParent()).setVisibility(View.GONE);
		}
		if (email.getText().length() == 0) {
			((View) email.getParent()).setVisibility(View.GONE);
		}
		if (phone.getText().length() == 0) {
			((View) phone.getParent()).setVisibility(View.GONE);
		}
		if (fax.getText().length() == 0) {
			((View) fax.getParent()).setVisibility(View.GONE);
		}
		if (secretary.getText().length() == 0) {
			((View) secretary.getParent()).setVisibility(View.GONE);
		}
		if (extraCaption.getText().length() == 0) {
			((View) extraCaption.getParent()).setVisibility(View.GONE);
		}
		if (extra.getText().length() == 0) {
			((View) extra.getParent()).setVisibility(View.GONE);
		}
		if (bib.getText().length() == 0) {
			((View) bib.getParent()).setVisibility(View.GONE);
		}
	}
}