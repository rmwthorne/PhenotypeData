var $ = jQuery;
$(document).ready(function () {
    var baseUrl = '//dev.mousephenotype.org/data';
    $.ajax({url: baseUrl + '/gostats',
        type: 'get',
        async: true,
        success: function (html) {
            $('div#goBlock').html(html);
        },
        error: function () {
            $('div#goBlock').html("Error loading GO annotation stats");
        }
    });

    // initialize sumo selection                   
    var holderTxt = 'Select dataset: multiple ok';
    $('.goselect').SumoSelect({
        placeholder: holderTxt,
        csvDispCount: 7
    });
    // some sanity checks for selecting checkboxes


    $('li[data-val=nogo], li[data-val=all],li[data-val=onlygo]').click(function () {
        if ($('li.selected').size() == 0) {
            $(this).removeClass('selected');
            var options = $('select option').size();

            for (var i = 0; i < options; i++) {
                $('.goselect')[0].sumo.unSelectItem(i);
            }
        }
        else {
            //console.log($(this).attr('data-val'));

            $('li.selected').removeClass('selected');
            $(this).addClass('selected');
            $('p.SlectBox span').text($(this).text());
        }


    });

    $('li[data-val=exp], li[data-val=ida],li[data-val=igi],li[data-val=imp], li[data-val=ipi],li[data-val=iso], li[data-val=iss]').click(function () {
        if ($('li.selected').attr('data-val') == 'nogo' || $('li.selected').attr('data-val') == 'onlygo' || $('li.selected').attr('data-val') == 'all') {
            $(this).removeClass('selected');
            alert('Sorry, selecting ' + $(this).text() + ' is disabled since you have selected either "No GO" or "Only GO" or "All dataset"');
            $('p.SlectBox span').text($('li.selected').text());
        }
    });

    var conf = {
        externalDbId: 1,
        fileType: '',
        fileName: '',
        solrCoreName: 'gene',
        params: '',
        gridFields: '',
        dumpMode: 'all',
        dogoterm: true
    };

    var commonQ = '(latest_phenotype_status:"Phenotyping Started" OR latest_phenotype_status:"Phenotyping Complete")';
    var commonFl = "mgi_accession_id,marker_symbol,go_term_id,go_term_evid,go_term_domain,go_term_name";
    var rows = 999999;
    var exportUrl = baseUrl + '/export';

    // submit form dynamically
    $('button').click(function () {
        var selectedStr = $('p.SlectBox span').text().trim();
        if (selectedStr == holderTxt) {
            alert("Sorry, you haven't selected dataset to export");
            return false;
        }

        conf.fileType = $(this).hasClass('tsv') ? 'tsv' : 'xls';
        conf.fileName = 'go_dump.' + conf.fileType;

        var qryMap = {
            "No GO": "q=" + commonQ + " AND -go_term_id:*&fl=mgi_accession_id,marker_symbol&rows=" + rows + "&wt=json",
            "All GO": "q=" + commonQ + " AND go_term_id:*&fl=" + commonFl + "&rows=" + rows + "&wt=json",
            "All dataset": "q=" + commonQ + "&fl=" + commonFl + "&rows=" + rows + "&wt=json"
        };
        if (selectedStr != 'No GO' && selectedStr != 'All GO' && selectedStr != 'All dataset') {
            var list = selectedStr.split(',');
            var aQ = [];
            for (var i = 0; i < list.length; i++) {
                aQ.push("go_term_evid:" + list[i].trim());
            }
            var evidParams = "(" + aQ.join(" OR ") + ")";

            conf.gridFields = commonFl;
            conf.params = "q=" + commonQ + " AND " + evidParams + "&fl=" + commonFl + "&rows=" + rows + "&wt=json";
        }
        else {
            if (selectedStr == 'No GO') {
                conf.gridFields = "mgi_accession_id,marker_symbol";
            }
            conf.params = qryMap[selectedStr.trim()];
        }

        var sInputs = '';
        var aParams = [];
        for (var k in conf) {
            aParams.push(k + "=" + conf[k]);
            sInputs += "<input type='text' name='" + k + "' value='" + conf[k] + "'>";
        }

        var form = "<form action='" + exportUrl + "' method=get>" + sInputs + "</form>";
        $(form).appendTo('body').submit().remove();

    });

});