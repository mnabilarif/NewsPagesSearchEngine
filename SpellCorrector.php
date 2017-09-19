<?php
/*
*************************************************************************** 
*   Copyright (C) 2008 by Felipe Ribeiro                                  * 
*   felipernb@gmail.com                                                   * 
*   http://www.feliperibeiro.com                                          * 
*                                                                         * 
*   Permission is hereby granted, free of charge, to any person obtaining * 
*   a copy of this software and associated documentation files (the       * 
*   "Software"), to deal in the Software without restriction, including   * 
*   without limitation the rights to use, copy, modify, merge, publish,   * 
*   distribute, sublicense, and/or sell copies of the Software, and to    * 
*   permit persons to whom the Software is furnished to do so, subject to * 
*   the following conditions:                                             * 
*                                                                         * 
*   The above copyright notice and this permission notice shall be        * 
*   included in all copies or substantial portions of the Software.       * 
*                                                                         * 
*   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       * 
*   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    * 
*   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.* 
*   IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR     * 
*   OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, * 
*   ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR * 
*   OTHER DEALINGS IN THE SOFTWARE.                                       * 
*************************************************************************** 
*/ 


/**
 * This class implements the Spell correcting feature, useful for the 
 * "Did you mean" functionality on the search engine. Using a dicionary of words
 * extracted from the product catalog.
 * 
 * Based on the concepts of Peter Norvig: http://norvig.com/spell-correct.html
 * 
 * @author Felipe Ribeiro <felipernb@gmail.com>
 * @date September 18th, 2008
 * @package catalog
 *
 */
class SpellCorrector {
	private static $NWORDS;
	
	/**
	 * Reads a text and extracts the list of words
	 *
	 * @param string $text
	 * @return array The list of words
	 */
	private static function words($text) {
		$matches = array();
        $tmp = preg_split("/[\s]+/", trim($text));
        $i = 0;
        foreach($tmp as $x=>$x_value) {
            $matches[$i++] = $x_value;
        }
        //echo count($matches)."<br>";
		return $matches;
	}
	
	/**
	 * Creates a table (dictionary) where the word is the key and the value is it's relevance 
	 * in the text (the number of times it appear)
	 *
	 * @param array $features
	 * @return array
	 */
	private static function train(array $features) {
		$model = array();
		$count = count($features);
		for($i = 0; $i<$count; $i++) {
			$f = $features[$i++];
			//$model[$f] +=1;
            $model[$f] = $features[$i];
		}
        //foreach($model as $x=>$x_value) {
            //echo "Key=" . $x . ", Value=" . $x_value;
            //echo "<br>";
        //}
		return $model;
	}
	
	/**
	 * Generates a list of possible "disturbances" on the passed string
	 *
	 * @param string $word
	 * @return array
	 */
	private static function edits1($word) {
		$alphabet = 'abcdefghijklmnopqrstuvwxyz';
		$alphabet = str_split($alphabet);
		$n = strlen($word);
		$edits = array();
		for($i = 0 ; $i<$n;$i++) {
			$edits[] = substr($word,0,$i).substr($word,$i+1); 		//deleting one char
			foreach($alphabet as $c) {
				$edits[] = substr($word,0,$i) . $c . substr($word,$i+1); //substituting one char
			}
		}
		for($i = 0; $i < $n-1; $i++) {
			$edits[] = substr($word,0,$i).$word[$i+1].$word[$i].substr($word,$i+2); //swapping chars order
		}
		for($i=0; $i < $n+1; $i++) {
			foreach($alphabet as $c) {
				$edits[] = substr($word,0,$i).$c.substr($word,$i); //inserting one char
			}
		}

		return $edits;
	}
	
	/**
	 * Generate possible "disturbances" in a second level that exist on the dictionary
	 *
	 * @param string $word
	 * @return array
	 */
	private static function known_edits2($word) {
		$known = array();
		foreach(self::edits1($word) as $e1) {
			foreach(self::edits1($e1) as $e2) {
				if(array_key_exists($e2,self::$NWORDS)) $known[] = $e2;				
			}
		}
		return $known;
	}
	
	/**
	 * Given a list of words, returns the subset that is present on the dictionary
	 *
	 * @param array $words
	 * @return array
	 */
	private static function known(array $words) {
		$known = array();
		foreach($words as $w) {
			if(array_key_exists($w,self::$NWORDS)) {
				$known[] = $w;

			}
		}
		return $known;
	}
	
    private static function myfunction($word) {
        $size = count(self::$NWORDS);
        $index = array_search($word, array_keys(self::$NWORDS));
        $allKeys = array_keys(self::$NWORDS);
        $max = 0;
        $result = $word;
        for ($i = 0; $i<11 && $index-$i>=0; $i++) {
            $tmp_word = $allKeys[$index-$i];
            $value = self::$NWORDS[$tmp_word];
            if( $value > $max) {
                //echo $c."  ".$value."<br>";
                $max = $value;
                $result = $tmp_word;
            }
        }
        for ($i = 1; $i<11 && $index+$i<$size; $i++) {
            $tmp_word = $allKeys[$index+$i];
            $value = self::$NWORDS[$tmp_word];
            if( $value > $max) {
                //echo $c."  ".$value."<br>";
                $max = $value;
                $result = $tmp_word;
            }
        }
        return $result;
    }
    
    private static function myfunction1($word) {
        $specialcheck = self::words(file_get_contents("wordProbabilityFile/specialword.txt"));
        $a = str_split($word);
        for ($i = 0; $i<count($specialcheck); $i++) {
            $res = $specialcheck[$i];
            $a1 = str_split($res);
            if (count(array_intersect($a, $a1)) == count($a)) {
                return $res;
            }
        }
        return false;
    }
    
    /**
	 * Returns the word that is present on the dictionary that is the most similar (and the most relevant) to the
	 * word passed as parameter, 
	 *
	 * @param string $word
	 * @return string
	 */
	public static function correct($word) {
        ini_set('memory_limit', '-1');
		$word = trim($word);
		if(empty($word)) return;
		$word = strtolower($word);
		
		//if(empty(self::$NWORDS)) {
			
			/* To optimize performance, the serialized dictionary can be saved on a file
			instead of parsing every single execution */
            
			if(!file_exists('serializedDictionaryFile/serialized_dictionary_of_'.$word[0].'.txt')) {
				self::$NWORDS = self::train(self::words(file_get_contents("wordProbabilityFile/".$word[0].".txt")));
                if (!file_exists('serializedDictionaryFile')) {
                    mkdir('serializedDictionaryFile', 0777, true);
                }
				$fp = fopen("serializedDictionaryFile/serialized_dictionary_of_".$word[0].".txt","w+");
				fwrite($fp,serialize(self::$NWORDS));
				fclose($fp);
			} else {
				self::$NWORDS = unserialize(file_get_contents("serializedDictionaryFile/serialized_dictionary_of_".$word[0].".txt"));
                
			}
		//}
		$candidates = array();
        $valueOfWord = 0;
		if(self::known(array($word))) {
            $valueOfWord = self::$NWORDS[$word];
			//return $word;
		} elseif(($tmp_candidates = self::known(self::edits1($word)))) {
			foreach($tmp_candidates as $candidate) {
				$candidates[] = $candidate;
			}
		} elseif(($tmp_candidates = self::known_edits2($word))) {
			foreach($tmp_candidates as $candidate) {
				$candidates[] = $candidate;
			}
		} else {
            return self::myfunction($word);
		}
        if (0 == count($candidates)) {
            return self::myfunction($word);
        } else {
            //echo count($candidates);
            $onechoice = self::myfunction1($word);
            if(!is_bool($onechoice)){
                return $onechoice;
            }
            $a = array("a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z");
            for ($k = 0; $k < 26; $k++) {
                for ($w = 0; $w < 26; $w++) {
                    $candidates[] = $a[$k].$a[$w].substr($word,2);
                }
            }
            $max = 0;
            $tmp = null;
            foreach($candidates as $c) {
                self::$NWORDS = unserialize(file_get_contents("serializedDictionaryFile/serialized_dictionary_of_".$c[0].".txt"));
                $value = self::$NWORDS[$c];
                if( $value > $max) {
                    $max = $value;
                    $tmp = $c;
                }
            }
            return $max > $valueOfWord? $tmp : $word;
        }
		//return $word;
	}
	
	
}

?>
