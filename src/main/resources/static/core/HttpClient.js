class HttpClient {
    constructor() {
        this.timeout = 10000;
    }

  get(_url, _requestParams, _success, _error) {
    let self = this;

    console.log('httpClient.get.'+_url+'.request',{
        url:_url,
        requestParams:_requestParams
    });

    $.ajax({
      type: 'GET',
      url: _url,
      data: _requestParams,
      timeout: this.timeout,
      beforeSend: function() {
        self.showLoadingBar();
      },
      success: function(response) {
        console.log('httpClient.get.'+_url+'.response',response);

        _success(response);
      },
      error: function(error) {
        _error(error);
      },
      complete: function() {
        self.hideLoadingBar();
      }
    });
  }

  post(_url, _requestParams, _requestBody, _success, _error, options) {
    let self = this;

    console.log('httpClient.post.'+_url+'.request',{
        url:_url,
        requestParams:_requestParams,
        requestBody:_requestBody
    });

    if(_requestParams != undefined && _requestParams != null && _requestParams != '') {
        let i=0;
        for(let key in _requestParams) {
            let value = _requestParams[key];
            _url += (i > 0) ? "&" : "?" + key + "=" + value;
            i++;
        }
    }

    $.ajax({
      type: 'POST',
      url: _url,
      dataType: 'json',
      contentType: 'application/json; charset=utf8',
      data: JSON.stringify(_requestBody),
      timeout: this.timeout,
      async: (options) ? options.async : true,
      beforeSend: function() {
        self.showLoadingBar();
      },
      success: function(response) {
        console.log('httpClient.post.'+_url+'.response',response);

        _success(response);
      },
      error: function(error) {
        _error(error);
      },
      complete: function() {
        self.hideLoadingBar();
      }
    });
  }

  put(_url, _requestParams, _requestBody, _success, _error) {
    let self = this;
    console.log('httpClient.put.'+_url+'.request',{
        url:_url,
        requestParams:_requestParams,
        requestBody:_requestBody
    });

    $.ajax({
      type: 'PUT',
      url: _url,
      dataType: 'json',
      contentType: 'application/json; charset=utf8',
      data: JSON.stringify(_requestBody),
      timeout: this.timeout,
      beforeSend: function() {
        self.showLoadingBar();
      },
      success: function(response) {
        console.log('httpClient.put.'+_url+'.response',response);

        _success(response);
      },
      error: function(error) {
        _error(error);
      },
      complete: function() {
        self.hideLoadingBar();
      }
    });
  }

  /**
   * 파일과 함께 워크플로우를 호출한다. (POST /workflow, multipart/form-data)
   * 요청메시지(JSON)는 'message' 파트로, 파일은 'files' 파트로 보낸다.
   * 업로드 파일은 요청메시지가 아니라 헤더(header.files)로 실려가고, FILE 노드(파일내용 인코딩 MULTIPART)가
   * 저장소에 스트림 그대로 저장한다. 파일경로에 쓸 값은 그 노드의 요청메시지로 보낸다.
   * 예) body:{uploadParam:[{boardId:'10'}]} + 파일경로 board/#{boardId}/#{original_filename}
   * _files 는 input[type=file] 의 files 또는 File 배열. 파일마다 한 번씩 저장하며,
   * 요청메시지 행이 한 개면 모든 파일이 그 값을 쓰고 여러 개면 파일 순서대로 짝지어진다.
   */
  postFiles(_url, _requestMessage, _files, _success, _error, _options) {
    let self = this;
    let options = _options || {};

    console.log('httpClient.postFiles.'+_url+'.request',{
        url:_url,
        requestMessage:_requestMessage,
        fileCount:(_files) ? _files.length : 0
    });

    let formData = new FormData();
    formData.append('message', JSON.stringify(_requestMessage));

    for(let i=0; i<((_files) ? _files.length : 0); i++) {
        formData.append('files', _files[i]);
    }

    $.ajax({
      type: 'POST',
      url: _url,
      data: formData,
      processData: false,
      contentType: false,
      timeout: options.timeout || this.timeout,
      beforeSend: function() {
        self.showLoadingBar();
      },
      success: function(response) {
        console.log('httpClient.postFiles.'+_url+'.response',response);

        _success(response);
      },
      error: function(error) {
        _error(error);
      },
      complete: function() {
        self.hideLoadingBar();
      }
    });
  }

  delete(_url, _requestParams, _success, _error) {
    let self = this;

    console.log('httpClient.delete.'+_url+'.request',{
        url:_url,
        requestParams:_requestParams
    });

    $.ajax({
      type: 'DELETE',
      url: _url,
      timeout: this.timeout,
      beforeSend: function() {
        self.showLoadingBar();
      },
      success: function(response) {
        console.log('httpClient.put.'+_url+'.response',response);

        _success(response);
      },
      error: function(error) {
        _error(error);
      },
      complete: function() {
        self.hideLoadingBar();
      }
    });
  }

    showLoadingBar() {
        App.loadingBar.show();
    }

    hideLoadingBar() {
        App.loadingBar.hide();
    }
}
window.App = window.App || {};
window.App.httpClient = new HttpClient();