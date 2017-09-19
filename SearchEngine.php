<?php
    // author: Tianfeng Ye;
    // make sure browsers see this page as utf-8 encoded HTML
    include 'simple_html_dom.php';
    header('Content-Type: text/html; charset=utf-8');
    $limit = 10;
    $query = isset($_REQUEST['q']) ? $_REQUEST['q'] : false;
    $mo = isset($_REQUEST['mode']) ? $_REQUEST['mode'] : false;
    $results = false;
    if ($query)
    {
        // The Apache Solr Client library should be on the include path
        // which is usually most easily accomplished by placing in the
        // same directory as this script ( . or current directory is a default
        // php include path entry in the php.ini)
        require_once('Apache/Solr/Service.php');
        //require_once('csci572hw4/SpellCorrector.php');
        // create a new solr service instance - host, port, and corename
        // path (all defaults in this example)
        $solr = new Apache_Solr_Service('localhost', 8983, '/solr/hw4/'); // should be modified here if want to make the code work
        // if magic quotes is enabled then stripslashes will be needed
        if (get_magic_quotes_gpc() == 1)
        {
            $query = stripslashes($query);
        }
        // in production code you'll always want to use a try /catch for any
        // possible exceptions emitted by searching (i.e. connection
        // problems or a query parsing error)
        try
        {
            if (strcmp($mo, "0") == 0)
            {
                $results = $solr->search($query, 0, $limit); // default mode
            }
            else
            {
                $results = $solr->search($query, 0, $limit, array('sort' => 'pageRankFile desc')); // pagerank mode, pass sort parameter
            }
        }
        catch (Exception $e)
        {
            // in production you'd probably log or email this error to an admin
            // and then show a special message to the user but for this example
            // we're going to show the full exception
            die("<html><head><title>SEARCH EXCEPTION</title><body><pre>{$e->__toString()}</pre></body></html>");
        }
    }
    ?>


<html>
    <head>
        <title>Homework4 of CS572 by Tianfeng Ye</title>
        <script src="http://code.jquery.com/jquery-1.12.4.js"></script>
        <script src="http://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>
        <link rel="stylesheet" href="https://code.jquery.com/ui/1.12.1/themes/excite-bike/jquery-ui.css">
        <style type="text/css">
            body
            {
                font-family: Times;
                background-color: rgb(252,251,239);
            }
            table.result
            {
                text-align: left;
            }
            table.input
            {
                width: 400px;
                text-align: center;
                margin-left: auto;
                margin-right: auto;
            }
            div#container
            {
                position: relative;
                text-align: center;
            }
            label.ex
            {
                font-family: life;
                font-size: 20px;
            }
        </style>
        <script>
        $(function() {
          $("#q").autocomplete({
                       source : function(request, response) {
                       var query = $("#q").val();
                       var lastword = query.toLowerCase().split(" ").pop(-1);
                       var url = "http://localhost:8983/solr/hw4/suggest?q=" + lastword + "&wt=json";
                       $.ajax({
                              url : url,
                              success : function(data) {
                              var suggestions = data.suggest.suggest[lastword].suggestions;
                              suggestions = $.map(suggestions, function (value, index) {
                                                  var query = $("#q").val();
                                                  var queries = query.split(" ");
                                                  var tmp = (value.term).substring(0, queries[queries.length - 1].length);
                                                  if (!/^[0-9a-zA-Z]+$/.test(value.term) || value.term.valueOf() == queries[queries.length - 1].toLowerCase().valueOf()) {
                                                    return null;
                                                  } else if (1 < queries.length && tmp.valueOf() == queries[queries.length - 1].toLowerCase().valueOf()) {
                                                    var prefix = query.substring(0, query.lastIndexOf(" ") + 1);
                                                    return prefix + ((queries[queries.length - 1].charAtIsUpper(0)) ? (value.term).capitalizeFirstLetter():value.term);
                                                  } else if (!isStopWord(value.term) && tmp.valueOf() == query.toLowerCase().valueOf()) {
                                                    return query.charAtIsUpper(0)? (value.term).capitalizeFirstLetter():value.term;
                                                  } else {
                                                    return null;
                                                  }
                                                  });
                                if (1 == query.trim().length) {
                                    response(suggestions.slice(0, 10));
                                } else if (2 == query.trim().length) {
                                    response(suggestions.slice(0, 6));
                                } else {
                                    response(suggestions.slice(0, 3));
                                }
                              },
                              dataType : 'jsonp',
                              jsonp : 'json.wrf'
                              });
                       },
                       minLength : 1
                       });
          });

        String.prototype.charAtIsUpper = function (atpos){
            var chr = this.charAt(atpos);
            return /[A-Z]|[\u0080-\u024F]/.test(chr) && chr === chr.toUpperCase();
        };

        String.prototype.capitalizeFirstLetter = function() {
            return this.charAt(0).toUpperCase() + this.slice(1);
        }

        function isStopWord(term) {
            var stopWordsList = "a,able,about,above,abst,accordance,according,accordingly,across,act,actually,added,adj,affected,affecting,affects,after,afterwards,again,\
                against,ah,all,almost,alone,along,already,also,although,always,am,among,amongst,an,and,announce,another,any,anybody,anyhow,anymore,anyone,\
                anything,anyway,anyways,anywhere,apparently,approximately,are,aren,arent,arise,around,as,aside,ask,asking,at,auth,available,away,awfully,b,\
                back,be,became,because,become,becomes,becoming,been,before,beforehand,begin,beginning,beginnings,begins,behind,being,believe,below,beside,\
                besides,between,beyond,biol,both,brief,briefly,but,by,c,ca,came,can,cannot,can't,cause,causes,certain,certainly,co,com,come,comes,contain,\
                containing,contains,could,couldnt,d,date,did,didn't,different,do,does,doesn't,doing,done,don't,down,downwards,due,during,e,each,ed,edu,\
                effect,eg,eight,eighty,either,else,elsewhere,end,ending,enough,especially,et,et-al,etc,even,ever,every,everybody,everyone,everything,\
                everywhere,ex,except,f,far,few,ff,fifth,first,five,fix,followed,following,follows,for,former,formerly,forth,found,four,from,further,furthermore,\
                g,gave,get,gets,getting,give,given,gives,giving,go,goes,gone,got,gotten,h,had,happens,hardly,has,hasn't,have,haven't,having,he,hed,hence,her,\
                here,hereafter,hereby,herein,heres,hereupon,hers,herself,hes,hi,hid,him,himself,his,hither,home,how,howbeit,however,hundred,i,id,ie,if,i'll,im,\
                immediate,immediately,importance,important,in,inc,indeed,index,information,instead,into,invention,inward,is,isn't,it,itd,it'll,its,itself,i've,\
                j,just,k,keep,keeps,kept,kg,km,know,known,knows,l,largely,last,lately,later,latter,latterly,least,less,lest,let,lets,like,liked,likely,line,\
                little,'ll,look,looking,looks,ltd,m,made,mainly,make,makes,many,may,maybe,me,mean,means,meantime,meanwhile,merely,mg,might,million,miss,ml,\
                more,moreover,most,mostly,mr,mrs,much,mug,must,my,myself,n,na,name,namely,nay,nd,near,nearly,necessarily,necessary,need,needs,neither,never,\
                nevertheless,new,next,nine,ninety,no,nobody,non,none,nonetheless,noone,nor,normally,nos,not,noted,nothing,now,nowhere,o,obtain,obtained,\
                obviously,of,off,often,oh,ok,okay,old,omitted,on,once,one,ones,only,onto,or,ord,other,others,otherwise,ought,our,ours,ourselves,out,outside,\
                over,overall,owing,own,p,page,pages,part,particular,particularly,past,per,perhaps,placed,please,plus,poorly,possible,possibly,potentially,pp,\
                predominantly,present,previously,primarily,probably,promptly,proud,provides,put,q,que,quickly,quite,qv,r,ran,rather,rd,re,readily,really,recent,\
                recently,ref,refs,regarding,regardless,regards,related,relatively,research,respectively,resulted,resulting,results,right,run,s,said,same,saw,\
                say,saying,says,sec,section,see,seeing,seem,seemed,seeming,seems,seen,self,selves,sent,seven,several,shall,she,shed,she'll,shes,should,\
                shouldn't,show,showed,shown,showns,shows,significant,significantly,similar,similarly,since,six,slightly,so,some,somebody,somehow,someone,\
                somethan,something,sometime,sometimes,somewhat,somewhere,soon,sorry,specifically,specified,specify,specifying,still,stop,strongly,sub,\
                substantially,successfully,such,sufficiently,suggest,sup,sure,t,take,taken,taking,tell,tends,th,than,thank,thanks,thanx,that,that'll,thats,\
                that've,the,their,theirs,them,themselves,then,thence,there,thereafter,thereby,thered,therefore,therein,there'll,thereof,therere,theres,thereto,\
                thereupon,there've,these,they,theyd,they'll,theyre,they've,think,this,those,thou,though,thoughh,thousand,throug,through,throughout,thru,thus,\
                til,tip,to,together,too,took,toward,towards,tried,tries,truly,try,trying,ts,twice,two,u,un,under,unfortunately,unless,unlike,unlikely,until,\
                unto,up,upon,ups,us,use,used,useful,usefully,usefulness,uses,using,usually,v,value,various,'ve,very,via,viz,vol,vols,vs,w,want,wants,was,\
                wasn't,way,we,wed,welcome,we'll,went,were,weren't,we've,what,whatever,what'll,whats,when,whence,whenever,where,whereafter,whereas,whereby,\
                wherein,wheres,whereupon,wherever,whether,which,while,whim,whither,who,whod,whoever,whole,who'll,whom,whomever,whos,whose,why,widely,willing,\
                wish,with,within,without,won't,words,world,would,wouldn't,www,x,y,yes,yet,you,youd,you'll,your,youre,yours,yourself,yourselves,you've,z,zero";
            var word = new RegExp("\\b"+term+"\\b","i");
            if (stopWordsList.search(word) < 0) {
                return false;
            } else {
                return true;
            }
        }
        </script>
    </head>
    <body>
        <div id="container">
            <table class="input" frame="box" bgcolor=#FFFFCC>
                <form accept-charset="utf-8" method="get">
                    <tr>
                    <td>
                        <label class="ex" for="q">Search:</label>
                    </td>
                    <td>
                        <input id="q" name="q" type="text" autocomplete="on" autofocus="autofocus" required="required" value="<?php echo htmlspecialchars($query, ENT_QUOTES, 'utf-8'); ?>"/>&nbsp;
                        <input type="radio" name="mode" value="0" <?php if (!isset($_REQUEST['mode']) || $_REQUEST['mode'] == '0') echo 'checked'; ?>/>Default&nbsp;
                        <input type="radio" name="mode" value="1" <?php if ($_REQUEST['mode'] == '1') echo 'checked'; ?>/>Page rank
                    </td>
                    </tr>
                    <tr>
                    <td></td><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="submit"/></td>
                    </tr>
                </form>
            </table>
        </div>

        <?php
            // display top 10 results
            if ($results)
            {
                $total = (int) $results->response->numFound;
                
                $start = min(1, $total);
                $end = min($limit, $total);
                if ($total < 11) {
                    include 'SpellCorrector.php';
        ?>
        <div id="container">
            <?php
                echo "Did you mean? ";
                $array = explode(" ", $query);
                $count = count($array);
                $new_query = array();
                if (!ctype_lower($array[0])) {
                        $new_query[0] = ucfirst(SpellCorrector::correct($array[0]));
                } else {
                        $new_query[0] = SpellCorrector::correct($array[0]);
                }
                for ($i = 1; $i < $count; $i++) {
                    if (!ctype_lower($array[$i])) {
                        $new_query[$i] = ucfirst(SpellCorrector::correct($array[$i]));
                    } else {
                        $new_query[$i] = SpellCorrector::correct($array[$i]);
                    }
                }
                }
            ?>
                <?php
                    $count = count($new_query);
                    echo '<a href="http://localhost/~tianfengye/hw4.php?q='.htmlentities($new_query[0]);
                    for ($i = 1; $i < $count; $i++) {
                        echo "+".htmlentities($new_query[$i]);
                    }
                    echo '&mode='.$mo.'">';
                    if (is_array($new_query)) {
                        foreach ($new_query as $v) {
                            if(strcmp($v, "Nato") == 0){
                              echo  "NATO ";
                            }else
                            echo $v." ";
                        }
                    }
                ?></a></div>

        <div style ="margin-left: 85px">Results <?php echo $start; ?> - <?php echo $end;?> of <?php echo $total; ?>:</div>
        <div style ="margin-left: 60px">
            <ol>
<?php
    class _String {
        private $string;
        private $startIndex;
        public $length;
        public function __construct($string) {
            $this->string = $string;
            $this->startIndex = 0;
            $this->length = strlen($string);
        }
        public function substr($from, $length = NULL) {
            $this->startIndex = $from;
            if ($length !== NULL) {
                //$this->endIndex = $from + $length;
                return substr($this->string, $from, $length);
            }
        }
        public function charAt($i){
            return $this->string[$i];
        }
    }

    function myfunction($id, $link, $query) {
        $res = array();
        //echo $id;
        $html = file_get_html("crawl_data/".$id);
        if (is_bool($html)) {
            return $res;
        }
        $title = $html->find('title', 0)->plaintext;
        $index = 0;
        $start = 0;
        $end = 0;
        $start1 = 0;
        $end1 = 0;
        //$_string = new _String();
        $pieces = explode(" ", $query);
        // echo count($pieces);
        $length = count($pieces);
        $index = stripos($title, $query);
        if (is_int(stripos($title, $query))) {
            $index = stripos($title, $query);
            $res[0] = substr($title, 0, $index);
            $res[1] = $query;
            $res[2] = substr($title, $index+strlen($query));
            return $res;
            //echo $title.'<br>';
        }
        elseif ($length > 1 && is_int(stripos($title, $pieces[0]))) {
            $index = stripos($title, $pieces[0]);
            $res[0] = substr($title, 0, $index);
            $res[1] = $pieces[0];
            $res[2] = substr($title, $index+strlen($pieces[0]));
            return $res;
        }
        elseif ($length > 1 && is_int(stripos($title, $pieces[1]))) {
            $index = stripos($title, $pieces[1]);
            $res[0] = substr($title, 0, $index);
            $res[1] = $pieces[1];
            $res[2] = substr($title, $index+strlen($pieces[1]));
            return $res;
        } else {
            $body = $html->getElementByTagName('body')->plaintext;
            $index = stripos($body, $query);
            //echo $index;
            //echo  substr($body,$index-50,100);
            if (is_int(stripos($body, $query))) {
                $_string = new _String($body);
                //echo $index;
                for ($i = $index; $i > 0; $i--) {
                    if(ctype_upper($_string->charAt($i))){
                        $start1 = $i;
                    }
                    if (preg_match('/^[\?\!\.]*$/', $_string->charAt($i)) && preg_match('/^[a-z]+$/', $_string->charAt($i - 1))) {
                        $start = $i + 1;
                        //echo $start."<br>";
                        break;
                    }
                }
                //echo $_string->length;
                for ($j = $index; 1; $j++) {
                    if(ctype_upper($_string->charAt($j))){
                        $end1 = $j;
                    }
                    if (preg_match('/^[\?\!\.]*$/', $_string->charAt($j)) && !preg_match('/^[A-Z]+$/', $_string->charAt($j - 1))
                        && !preg_match('/^[A-Z]+$/', $_string->charAt($j - 2))) {
                        $end = $j + 1;
                        //echo $end."<br>";
                        break;
                    }
                }
                // if(preg_match('/(?<![A-Z])\./', $_string->charAt($j))){
                if ($end - $index > 200) {
                    $end = $end1;
                }
                if ($index - $start > 200) {
                    $start = $start1;
                }
                //echo $_string->substr($start, $end+50 - $start)."<br>";
                $tmp = trim($_string->substr($start, $end - $start));
                $res[0] = substr($tmp, 0, $index - $start - 1);
                $res[1] = $query;
                $res[2] = substr($tmp, $index - $start - 1 + strlen($query));
                //echo $_string->substr($start, $end - $start)."<br>";
                return $res;
            } else {
                if ($length > 1) {
                    for ($k = 0; $k < $length; $k++) {
                        $index = stripos($body, $pieces[$k]);
                        if (is_int(stripos($body, $pieces[$k]))) {
                            $_string = new _String($body);
                            for ($i = $index; $i > 0; $i--) {
                                if(ctype_upper($_string->charAt($i))){
                                    $start1 = $i;
                                }
                                if (preg_match('/^[\?\!\.]*$/', $_string->charAt($i))&& preg_match('/^[a-z]+$/', $_string->charAt($i - 1))) {
                                    $start = $i + 1;
                                    //echo $start."<br>";
                                    break;
                                }
                            }
                            for ($j = $index; 1; $j++) {
                                if(ctype_upper($_string->charAt($j))){
                                    $end1 = $j;
                                }
                                if (preg_match('/^[\?\!\.]*$/', $_string->charAt($j)) && !preg_match('/^[A-Z]+$/', $_string->charAt($j - 1))
                                    && !preg_match('/^[A-Z]+$/', $_string->charAt($j - 2))) {
                                    $end = $j + 1;
                                    //echo $end."<br>";
                                    break;
                                }
                            }
                            if ($end - $index > 200) {
                                $end = $end1;
                            }
                            if ($index - $start > 200) {
                                $start = $start1;
                            }
                            //echo $_string->substr($start, $end - $start)."<br>";
                            $tmp = trim($_string->substr($start, $end - $start));
                            $res[0] = substr($tmp, 0, $index - $start - 1);
                            $res[1] = $pieces[$k];
                            $res[2] = substr($tmp, $index - $start - 1 + strlen($pieces[$k]));
                            $w = strlen($pieces[$k]);
                            if ($res[2][0] == $pieces[$k][$w-1]) {
                                $res[2] = substr($res[2], 1);
                            }
                            return $res;
                            //echo $_string->substr($start, $end - $start)."<br>";
                            //break;
                        }
                    }
                } else {
                    return $res; //No snippets
                }
            }
        }
    }
?>
            <?php
                // iterate result documents
                foreach ($results->response->docs as $doc)
                {
            ?>
                <li>
                    <table class="result" width="1200" border="1" cellspacing="0">
                    <?php
                        // iterate document fields / values and store the value of four fields:  id, title, author, description
                        // if the field does not exists, use default value this is N/A
                        // show them in a table
                        $title = "N/A";
                        $author = "N/A";
                        $description = "N/A";
                        $link = "N/A";
                        $id = "N/A";
                        $snippet = array();
                        foreach ($doc as $field => $value)
                        {
                            if (strcmp($field, "id") == 0)
                            {
                                $id = htmlspecialchars($value, ENT_NOQUOTES, 'utf-8');
                            }
                            elseif (strcmp($field, "author") == 0)
                            {
                                $author = htmlspecialchars($value, ENT_NOQUOTES, 'utf-8');
                            }
                            elseif (strcmp($field, "description") == 0)
                            {
                                $description = htmlspecialchars($value, ENT_NOQUOTES, 'utf-8');
                            }
                            elseif (strcmp($field, "title") == 0)
                            {
                                if (!is_string($value)) {
                                    $title = htmlspecialchars($value[0], ENT_NOQUOTES, 'utf-8');
                                }
                                else {
                                    $title = htmlspecialchars($value, ENT_NOQUOTES, 'utf-8');
                                }
                            }
                        }
                        // get the corresponding actual URL from mapping csv file
                        // but need to process the id to get the filename used to save the html webpage first
                        // 11 equals to the length of the name of the folder that stored all the crawled pages(here is crawl_data) and plus 1(because of /)
                        $pos = strpos($id, "data"); // should be modified here if want to make the code work
                        $id = substr($id, $pos + 5); // should be modified here if want to make the code work
                        $found = false;
                        $file = fopen("mapCNNFile.csv", "r"); // iterate mapCNNFile.csv first
                        
                        //echo count();
                        while (! feof($file))
                        {
                            $array = fgetcsv($file);
                            if (strcmp($array[0], $id) == 0)
                            {
                                $link = $array[1];
                                $found = true;
                                $snippet = myfunction($id, $link, $query);
                                break;
                            }
                        }
                        fclose($file);
                        if (! $found) {
                            $file = fopen("mapUSATodayFile.csv", "r"); // the result is from USAToday
                            while (! feof($file))
                            {
                                $array = fgetcsv($file);
                                if (strcmp($array[0], $id) == 0)
                                {
                                    $link = $array[1];
                                    $snippet = myfunction($id, $link, $query);
                                    break;
                                }
                            }
                            fclose($file);
                        }
                    ?>
                        <tr><th width="120"><?php echo "Title"; ?></th><td><?php echo $title; ?></td><tr>
                        <tr><th width="120"><?php echo "Author"; ?></th><td><?php echo $author; ?></td><tr>
                        <tr><th width="120"><?php echo "Description"; ?></th><td><?php echo $description; ?></td><tr>
                        <tr><th width="120"><?php echo "Link"; ?></th><td><?php echo '<a target = "_blank" href = "' . $link . '">' . $link . '</a>';?></td><tr>
                        <tr><th width="120"><?php echo "Snippets"; ?></th><td><?php
                            if (empty($snippet)) { // default if not found
                                echo $title;
                            }
                            else {
                                echo trim($snippet[0])."<span style='font-weight: bold'> ".$snippet[1]."</span> ".trim($snippet[2]);
                            }
                                //$file = 'output.txt';
                                // Open the file to get existing content
                                //$current = file_get_contents($file);

                                // Append a new person to the file
                                //$current .= $tmp. ".". $link. "\n";
                                //$tmp++;
                                // Write the contents back to the file
                                //file_put_contents($file, $current);
                            
                            ?></td><tr>
                    </table>
                </li>
                <br>
            <?php
                }
            ?>
            </ol>
        </div>
    <?php
        }
    ?>

    </body>
</html>
