<#escape x as x?html>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <style type="text/css">
        body {
            margin: 0 auto;
            width: 900px;
            background-color: #CCB;
        }

        .container {
            width: 700px;
            height: 700px;
            margin: 0 auto;
        }

        img {
            width: auto;
            height: auto;
            max-width: 100%;
            max-height: 100%;
            padding-bottom: 36px;
        }

        p {
            display: block;
            font-size: 20px;
            color: blue;
        }
    </style>
</head>
<body>
<div class="container">
    <img src="images/sorry2.png" />
    <p>
        <span style="color: red; display: inline;">${errorMsg!""} </span><br>
    </p>
</div>
</body>
</html>
</#escape>