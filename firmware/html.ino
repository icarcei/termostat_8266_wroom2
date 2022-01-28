void gethead(char* message, char* title){
  sprintf(message, "\
<head>\
<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\
<title>%s</title>\
<style>\
body {\
width: 700px;\
margin: 40px auto;\
font-family: 'trebuchet MS', 'Lucida sans', Arial;\
font-size: 14px;\
color: #444;\
}\
table {\
*border-collapse: collapse;\
border-spacing: 0;\
width: 100%;\
}\
footer{\
position:fixed;\
bottom:0px;\
heigh:50px;\
}\
.zebra td, .zebra th {\
padding: 10px;\
border-bottom: 1px solid #f2f2f2;\
}\
.zebra tbody tr:nth-child(even) {\
background: #f5f5f5;\
-webkit-box-shadow: 0 1px 0 rgba(255,255,255,.8) inset;\
-moz-box-shadow:0 1px 0 rgba(255,255,255,.8) inset;\
box-shadow: 0 1px 0 rgba(255,255,255,.8) inset;\
}\
</style>\
</head> ", title);
}

