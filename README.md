# NFC QSL Android
NFC QSL is an Android application which can use an NFC tag as an **amateur radio QSL info tag**. 
It can read QSL information from NFC tag such as NXP ntag 215, write your own QSL info and set a password to protect your QSL info against tampering.

## How to use it
Firstly get an QSL with NFC tag (or an empty NFC tag such as ntag 215), open NFC QSL and scan that NFC tag. Then QSL information (or "Empty tag" words) will appear on your phone.
![1](readme_pic/1.jpg)
![2](readme_pic/2.jpg)

Then push "Write this QSL" to fill the blanks of QSO info.
![3](readme_pic/3.jpg)

When "Write QSL" button is pushed, QSL info will be written into the NFC tag. Then re-scan the NFC tag, your QSL information will display again.

You can also use "Lock this QSL" to set a password to prevent tampering.
![4](readme_pic/4.jpg)

After locking, the QSL info will protected. You can only change QSL info after "Unlocking" the NFC tag.
![5](readme_pic/5.jpg)

## Supported NFC tag type
- NXP Ntag 213/215/216

## Further to do
- Information CRC verification
- ADIF input/output
- Other NFC chips supports