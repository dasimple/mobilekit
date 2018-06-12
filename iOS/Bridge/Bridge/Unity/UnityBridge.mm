#import <UIKit/UIKit.h>

extern "C"
{
	float iosBridge_getPixelDensity()
	{
		return [[UIScreen mainScreen] scale];
	}
	
	void iosBridge_visitFacebookPage()
	{
		NSURL *url = [NSURL URLWithString: @"fb://profile/515329572153315"];
		if(![[UIApplication sharedApplication] canOpenURL: url])
		{
			url = [NSURL URLWithString: @"https://www.facebook.com/earthexploregame/"];
		}
		[[UIApplication sharedApplication] openURL: url];
	}
}
