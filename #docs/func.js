/* global window, define, module */
(function(global, factory) {
  var Gauge = factory(global);
  if (typeof define === "function" && define.amd) {
    // AMD support
    define(function() {
      return Gauge;
    });
  } else if (typeof module === "object" && module.exports) {
    // CommonJS support
    module.exports = Gauge;
  } else {
    // We are probably running in the browser
    global.Gauge = Gauge;
  }
})(typeof window === "undefined" ? this : window, function(global, undefined) {
  var document = global.document,
    slice = Array.prototype.slice,
    requestAnimationFrame =
      global.requestAnimationFrame ||
      global.mozRequestAnimationFrame ||
      global.webkitRequestAnimationFrame ||
      global.msRequestAnimationFrame ||
      function(cb) {
        return setTimeout(cb, 1000 / 60);
      };

  // EXPERIMENTAL!!
  /**
   * Simplistic animation function for animating the gauge. That's all!
   * Options are:
   * {
   *  duration: 1,    // In seconds
   *  start: 0,       // The start value
   *  end: 100,       // The end value
   *  step: function, // REQUIRED! The step function that will be passed the value and does something
   *  easing: function // The easing function. Default is easeInOutCubic
   * }
   */
  function Animation(options) {
    var duration = options.duration,
      currentIteration = 1,
      iterations = 60 * duration,
      start = options.start || 0,
      end = options.end,
      change = end - start,
      step = options.step,
      easing =
        options.easing ||
        function easeInOutCubic(pos) {
          // https://github.com/danro/easing-js/blob/master/easing.js
          if ((pos /= 0.5) < 1) return 0.5 * Math.pow(pos, 3);
          return 0.5 * (Math.pow(pos - 2, 3) + 2);
        };

    function animate() {
      var progress = currentIteration / iterations,
        value = change * easing(progress) + start;
      // console.log(progress + ", " + value);
      step(value, currentIteration);
      currentIteration += 1;

      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    }
    // start!
    requestAnimationFrame(animate);
  }

  var Gauge = (function() {
    var SVG_NS = "http://www.w3.org/2000/svg";

    var GaugeDefaults = {
      centerX: 50,
      centerY: 50
    };

    var defaultOptions = {
      dialRadius: 40,
      dialStartAngle: 135,
      dialEndAngle: 45,
      value: 0,
      max: 100,
      min: 0,
      valueDialClass: "value",
      valueClass: "value-text",
      dialClass: "dial",
      gaugeClass: "gauge",
      showValue: true,
      gaugeColor: null,
      label: function(val) {
        return Math.round(val);
      }
    };

    function shallowCopy(/* source, ...targets*/) {
      var target = arguments[0],
        sources = slice.call(arguments, 1);
      sources.forEach(function(s) {
        for (k in s) {
          if (s.hasOwnProperty(k)) {
            target[k] = s[k];
          }
        }
      });
      return target;
    }

    /**
     * A utility function to create SVG dom tree
     * @param {String} name The SVG element name
     * @param {Object} attrs The attributes as they appear in DOM e.g. stroke-width and not strokeWidth
     * @param {Array} children An array of children (can be created by this same function)
     * @return The SVG element
     */
    function svg(name, attrs, children) {
      var elem = document.createElementNS(SVG_NS, name);
      for (var attrName in attrs) {
        elem.setAttribute(attrName, attrs[attrName]);
      }

      if (children) {
        children.forEach(function(c) {
          elem.appendChild(c);
        });
      }
      return elem;
    }

    /**
     * Translates percentage value to angle. e.g. If gauge span angle is 180deg, then 50%
     * will be 90deg
     */
    function getAngle(percentage, gaugeSpanAngle) {
      return percentage * gaugeSpanAngle / 100;
    }

    function normalize(value, min, limit) {
      var val = Number(value);
      if (val > limit) return limit;
      if (val < min) return min;
      return val;
    }

    function getValueInPercentage(value, min, max) {
      const newMax = max - min,
        newVal = value - min;
      return 100 * newVal / newMax;
      // var absMin = Math.abs(min);
      // return 100 * (absMin + value) / (max + absMin);
    }

    /**
     * Gets cartesian co-ordinates for a specified radius and angle (in degrees)
     * @param cx {Number} The center x co-oriinate
     * @param cy {Number} The center y co-ordinate
     * @param radius {Number} The radius of the circle
     * @param angle {Number} The angle in degrees
     * @return An object with x,y co-ordinates
     */
    function getCartesian(cx, cy, radius, angle) {
      var rad = angle * Math.PI / 180;
      return {
        x: Math.round((cx + radius * Math.cos(rad)) * 1000) / 1000,
        y: Math.round((cy + radius * Math.sin(rad)) * 1000) / 1000
      };
    }

    // Returns start and end points for dial
    // i.e. starts at 135deg ends at 45deg with large arc flag
    // REMEMBER!! angle=0 starts on X axis and then increases clockwise
    function getDialCoords(radius, startAngle, endAngle) {
      var cx = GaugeDefaults.centerX,
        cy = GaugeDefaults.centerY;
      return {
        end: getCartesian(cx, cy, radius, endAngle),
        start: getCartesian(cx, cy, radius, startAngle)
      };
    }

    /**
     * Creates a Gauge object. This should be called without the 'new' operator. Various options
     * can be passed for the gauge:
     * {
     *    dialStartAngle: The angle to start the dial. MUST be greater than dialEndAngle. Default 135deg
     *    dialEndAngle: The angle to end the dial. Default 45deg
     *    radius: The gauge's radius. Default 400
     *    max: The maximum value of the gauge. Default 100
     *    value: The starting value of the gauge. Default 0
     *    label: The function on how to render the center label (Should return a value)
     * }
     * @param {Element} elem The DOM into which to render the gauge
     * @param {Object} opts The gauge options
     * @return a Gauge object
     */
    return function Gauge(elem, opts) {
      opts = shallowCopy({}, defaultOptions, opts);
      var gaugeContainer = elem,
        limit = opts.max,
        min = opts.min,
        value = normalize(opts.value, min, limit),
        radius = opts.dialRadius,
        displayValue = opts.showValue,
        startAngle = opts.dialStartAngle,
        endAngle = opts.dialEndAngle,
        valueDialClass = opts.valueDialClass,
        valueTextClass = opts.valueClass,
        valueLabelClass = opts.valueLabelClass,
        dialClass = opts.dialClass,
        gaugeClass = opts.gaugeClass,
        gaugeColor = opts.color,
        gaugeValueElem,
        gaugeValuePath,
        label = opts.label,
        instance;

      if (startAngle < endAngle) {
        console.log("WARN! startAngle < endAngle, Swapping");
        var tmp = startAngle;
        startAngle = endAngle;
        endAngle = tmp;
      }

      function pathString(radius, startAngle, endAngle, largeArc) {
        var coords = getDialCoords(radius, startAngle, endAngle),
          start = coords.start,
          end = coords.end,
          largeArcFlag = typeof largeArc === "undefined" ? 1 : largeArc;

        return [
          "M",
          start.x,
          start.y,
          "A",
          radius,
          radius,
          0,
          largeArcFlag,
          1,
          end.x,
          end.y
        ].join(" ");
      }

      function initializeGauge(elem) {
        gaugeValueElem = svg("text", {
          x: 50,
          y: 50,
          fill: "#999",
          class: valueTextClass,
          "font-size": "100%",
          "font-family": "sans-serif",
          "font-weight": "normal",
          "text-anchor": "middle"
        });

        gaugeValuePath = svg("path", {
          class: valueDialClass,
          fill: "none",
          stroke: "#666",
          "stroke-width": 2.5,
          d: pathString(radius, startAngle, startAngle) // value of 0
        });

        var angle = getAngle(100, 360 - Math.abs(startAngle - endAngle));
        var flag = angle <= 180 ? 0 : 1;
        var gaugeElement = svg(
          "svg",
          { viewBox: "0 0 100 100", class: gaugeClass },
          [
            svg("path", {
              class: dialClass,
              fill: "none",
              stroke: "#eee",
              "stroke-width": 2,
              d: pathString(radius, startAngle, endAngle, flag)
            }),
            gaugeValueElem,
            gaugeValuePath
          ]
        );
        elem.appendChild(gaugeElement);
      }

      function updateGauge(theValue, frame) {
        var val = getValueInPercentage(theValue, min, limit),
          // angle = getAngle(val, 360 - Math.abs(endAngle - startAngle)),
          angle = getAngle(val, 360 - Math.abs(startAngle - endAngle)),
          // this is because we are using arc greater than 180deg
          flag = angle <= 180 ? 0 : 1;
        if (displayValue) {
          if (label.length === 1) {
            gaugeValueElem.textContent = (((theValue + Number.EPSILON) * 100) / 100 ).toFixed(1)+"0 ºC";
          }
        }
        gaugeValuePath.setAttribute(
          "d",
          pathString(radius, startAngle, angle + startAngle, flag)
        );
      }

      function setGaugeColor(value, duration) {
        var c = gaugeColor(value),
          dur = duration * 1000,
          pathTransition = "stroke " + dur + "ms ease";
        // textTransition = "fill " + dur + "ms ease";

        gaugeValuePath.style = [
          "stroke: " + c,
          "-webkit-transition: " + pathTransition,
          "-moz-transition: " + pathTransition,
          "transition: " + pathTransition
        ].join(";");
        /*
        gaugeValueElem.style = [
          "fill: " + c,
          "-webkit-transition: " + textTransition,
          "-moz-transition: " + textTransition,
          "transition: " + textTransition,
        ].join(";");
        */
      }

      instance = {
        setMaxValue: function(max) {
          limit = max;
        },
        setValue: function(val) {
          value = normalize(val, min, limit);
          if (gaugeColor) {
            setGaugeColor(value, 0);
          }
          updateGauge(value);
        },
        setValueAnimated: function(val, duration) {
          var oldVal = value;
          value = normalize(val, min, limit);
          if (oldVal === value) {
            return;
          }

          if (gaugeColor) {
            setGaugeColor(value, duration);
          }
          Animation({
            start: oldVal || 0,
            end: value,
            duration: duration || 1,
            step: function(val, frame) {
              updateGauge(val, frame);
            }
          });
        },
        getValue: function() {
          return value;
        }
      };
      
      initializeGauge(gaugeContainer);
      instance.setValue(value);
      return instance;
    };
  })();

  return Gauge;
});


var gauge2 = Gauge(document.getElementById("gauge2"), {
  max: 40,
  dialStartAngle: -90,
  dialEndAngle: -90.001,
  value: 00,
  /*label: function(value) {
    return Math.round(value * 40) / 40;
  }*/
});

/*(function loop() {
  var value1 = Math.random() * 100,
    value2 = Math.random() * 100;

  // setValueAnimated(value, durationInSecs);
  gauge2.setValueAnimated(value2, 2);

  window.setTimeout(loop, 6000);
})();*/

//plugin bootstrap minus and plus
//http://jsfiddle.net/laelitenetwork/puJ6G/
$('.btn-number').click(function(e){
    e.preventDefault();
    
    fieldName = $(this).attr('data-field');
    type      = $(this).attr('data-type');
    var input = $("input[name='"+fieldName+"']");
    var currentVal = parseInt(input.val());
    if (!isNaN(currentVal)) {
        if(type == 'minus') {
            
            if(currentVal > input.attr('min')) {
                input.val(currentVal - 1).change();
            } 
            if(parseInt(input.val()) == input.attr('min')) {
                $(this).attr('disabled', true);
            }

        } else if(type == 'plus') {

            if(currentVal < input.attr('max')) {
                input.val(currentVal + 1).change();
            }
            if(parseInt(input.val()) == input.attr('max')) {
                $(this).attr('disabled', true);
            }

        }else if(type == 'desliga'){
            postData(2,"linha1",false);

        }else if(type == 'liga'){
            postData(2,"linha1",true);
        }
    } else {
        input.val(0);
    }
});
$('.input-number').focusin(function(){
   $(this).data('oldValue', $(this).val());
});
$('.input-number').change(function() {
    
    minValue =  parseInt($(this).attr('min'));
    maxValue =  parseInt($(this).attr('max'));
    valueCurrent = parseInt($(this).val());
    console.log(valueCurrent);
    postData(1,"tempPROG", valueCurrent);
    
    name = $(this).attr('name');
    if(valueCurrent >= minValue) {
        $(".btn-number[data-type='minus'][data-field='"+name+"']").removeAttr('disabled')
    } else {
        alert('Sorry, the minimum value was reached');
        $(this).val($(this).data('oldValue'));
    }
    if(valueCurrent <= maxValue) {
        $(".btn-number[data-type='plus'][data-field='"+name+"']").removeAttr('disabled')
    } else {
        alert('Sorry, the maximum value was reached');
        $(this).val($(this).data('oldValue'));
    }
    
    
});
$(".input-number").keydown(function (e) {
        // Allow: backspace, delete, tab, escape, enter and .
        if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 190]) !== -1 ||
             // Allow: Ctrl+A
            (e.keyCode == 65 && e.ctrlKey === true) || 
             // Allow: home, end, left, right
            (e.keyCode >= 35 && e.keyCode <= 39)) {
                 // let it happen, don't do anything
                 return;
        }
        // Ensure that it is a number and stop the keypress
        if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105)) {
            e.preventDefault();
        }
    });

    function postData(type, key, value) {
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
          if (this.readyState == 4 && this.status == 200) {
              if(type == 1){
                document.getElementById("tempPROG").innerHTML =
                json[key].toFixed(2);
              }else if(type == 2){
                if(json.linha1==true){
                    document.getElementById("status_linha_1").innerHTML=" - Ligada!";
                    document.getElementById("btnliga").style.display = "none";
                    document.getElementById("btndesliga").style.display = "block";
                    document.getElementById("btndesliga").style.display = "inline";
                }else{
                    document.getElementById("status_linha_1").innerHTML=" - Desligada!";
                    document.getElementById("btnliga").style.display = "block";
                    document.getElementById("btnliga").style.display = "inline";
                    document.getElementById("btndesliga").style.display = "none";
                }
            }
            
          }
        };
        var json = {};
        if(type == 1){
            json[key] = value;
        }else if(type == 2){
            json[key] = value;
        }
        
        console.log(json);
        
        xhttp.open("POST", "http://192.168.4.1/post", true);
        xhttp.send(JSON.stringify(json));
      }


      function formatHour(number){
        if(parseInt(number) < 10){
            return "0"+parseInt(number).toString();
        }
        return number;

        }

      function attHour(){
        var data = new Date();

        // Guarda cada pedaço em uma variável
        var dia     = data.getDate();           // 1-31
        var dia_sem = data.getDay();            // 0-6 (zero=domingo)
        var mes     = data.getMonth();          // 0-11 (zero=janeiro)
        var ano2    = data.getYear()-100;           // 2 dígitos
        var ano4    = data.getFullYear();       // 4 dígitos
        var hora    = data.getHours();          // 0-23
        var min     = data.getMinutes();        // 0-59
        var seg     = data.getSeconds();        // 0-59
        var mseg    = data.getMilliseconds();   // 0-999
        var tz      = data.getTimezoneOffset(); // em minutos
        
        // Formata a data e a hora (note o mês + 1)
        var str_data = formatHour(dia)+ '/' + formatHour((mes+1))+ '/' +formatHour(ano2)
        var str_hora = formatHour(hora) + ':' + formatHour(min) + ':' + formatHour(seg);
        document.getElementById("hora").innerHTML=str_hora +' '+ str_data;
        
      }

      

      function getData() {
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
          if (this.readyState == 4 && this.status == 200) {
            //document.getElementById("demo").innerHTML =  this.responseText;
            const json = JSON.parse(this.responseText);
            document.getElementById("tempPROG").innerHTML =
            json.tempPROG.toFixed(2);
            gauge2.setValueAnimated(json.sensor1, 2);
            if(json.linha1==true){
                document.getElementById("status_linha_1").innerHTML=" - Ligada!";
                document.getElementById("btnliga").style.display = "none";
                document.getElementById("btndesliga").style.display = "block";
                document.getElementById("btndesliga").style.display = "inline";
            }else{
                document.getElementById("status_linha_1").innerHTML=" - Desligada!";
                document.getElementById("btnliga").style.display = "block";
                document.getElementById("btnliga").style.display = "inline";
                document.getElementById("btndesliga").style.display = "none";
            }
            attHour();

          }
        };
        xhttp.open("GET", "http://192.168.4.1/get", true);
        xhttp.send();
      }

    $( document ).ready(function() {
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
          if (this.readyState == 4 && this.status == 200) {
            const json = JSON.parse(this.responseText);
            document.getElementById("temp1").value = json.tempPROG;
           

          }
        };
        xhttp.open("GET", "http://192.168.4.1/get", true);
        xhttp.send();
        setInterval(getData, 5000);
    });