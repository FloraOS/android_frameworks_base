package com.android.internal.pm.ext;

import android.annotation.Nullable;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.ext.PackageId;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.pm.pkg.parsing.ParsingPackage;
import com.android.internal.pm.pkg.parsing.ParsingPackageUtils;

import com.android.internal.pm.parsing.pkg.PackageImpl;

import libcore.util.HexEncoding;

public class GMSPackageValidator {
    private static final String TAG = GMSPackageValidator.class.getSimpleName();

    private final ParseInput input;
    private final ParsingPackage parsingPackage;
    private final PackageImpl pkg;
    
    @Nullable
    private ParseResult<SigningDetails> signingDetailsParseResult;

    public GMSPackageValidator(ParsingPackage parsingPackage, ParseInput input) {
        this.input = input;
	this.parsingPackage = parsingPackage;
	this.pkg = (PackageImpl) parsingPackage;
    }

    public boolean validate() {
        String packageName = pkg.getPackageName();

        // Guard if the package is a GMS package
        if (!isGmsPackage(packageName)) {
            return true;
        }

        // Validate the package's signature
        return validateSignature(packageName);
    }

    private boolean isGmsPackage(String packageName) {
        return switch (packageName) {
            case PackageId.GMS_CORE_NAME, 
                 PackageId.PLAY_STORE_NAME, 
                 PackageId.G_SEARCH_APP_NAME, 
                 PackageId.ANDROID_AUTO_NAME, 
                 PackageId.G_TEXT_TO_SPEECH_NAME -> true;
            default -> false;
        };
    }

    private boolean validateSignature(String packageName) {
        SigningDetails signingDetails = pkg.getSigningDetails();

        if (signingDetails == SigningDetails.UNKNOWN) {
            final ParseResult<SigningDetails> result = ParsingPackageUtils.parseSigningDetails(input, parsingPackage);
            signingDetailsParseResult = result;

            if (result.isError()) {
                Log.e(TAG, "unable to parse SigningDetails for " + parsingPackage.getPackageName()
                        + "; code " + result.getErrorCode() + "; msg " + result.getErrorMessage(),
                        result.getException());
                return false;
            }

            signingDetails = result.getResult();
	}

        String[] validCertificates = getValidCertificates(packageName);
        if (validCertificates == null) {
            Log.e(TAG, "No valid certificates configured for package: " + packageName);
            return false;
        }

        for (String certSha256String : validCertificates) {
            byte[] validCertSha256 = HexEncoding.decode(certSha256String);
            if (signingDetails.hasSha256Certificate(validCertSha256)) {
                Log.i(TAG, "Package " + packageName + " passed signature validation.");
                return true;
            }
        }

        Log.w(TAG, "Package " + packageName + " failed signature validation.");
        return false;
    }

    private String[] getValidCertificates(String packageName) {
        return switch (packageName) {
            case PackageId.GMS_CORE_NAME, PackageId.PLAY_STORE_NAME, PackageId.G_SEARCH_APP_NAME -> mainGmsCerts();
            case PackageId.G_CAMERA_NAME -> new String[] {
                "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
                "1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00"
            };
            case PackageId.PIXEL_CAMERA_SERVICES_NAME -> new String[] {
                "226bb0439d6baeaa5a397c586e7031d8addfaec73c65be212f4a5dbfbf621b92"
            };
            case PackageId.ANDROID_AUTO_NAME -> new String[] {
                "1ca8dcc0bed3cbd872d2cb791200c0292ca9975768a82d676b8b424fb65b5295"
            };
            case PackageId.TYCHO_NAME -> new String[] {
                "8c4e8f364cb132d41626f67749a6385605f51d365098c0cb5976eb5c1500a3ce"
            };
            default -> null;
        };
    }

    private static String[] mainGmsCerts() {
        return new String[] {
            "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
            "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
            "1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00"
        };
    }
}

