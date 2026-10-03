class ModalPopup {
    constructor() {
        this.POPUP_ID = 'popup';
    }

    open(url, options = {}, params) {
        const settings = {
            title: options.title || '',
            size: options.size || 'modal-xl',
            height: options.height || '80vh',
            messageId: options.messageId || 'POPUP_REQUEST'
        };

        let $popup = $('#' + this.POPUP_ID);

        if ($popup.length === 0) {

            $popup = $('<div>')
                .addClass('modal fade')
                .attr({
                    id: this.POPUP_ID,
                    tabindex: '-1',
                    'aria-hidden': 'true'
                });

            const $dialog = $('<div>')
                .addClass('modal-dialog');

            const $content = $('<div>')
                .addClass('modal-content');

            const $header = $('<div>')
                .addClass('modal-header');

            const $title = $('<h5>')
                .addClass('modal-title')
                .attr('id', 'popup-title');

            const $closeBtn = $('<button>')
                .addClass('close')
                .attr({
                    type: 'button',
                    'data-dismiss': 'modal'
                })
                .html('<span>&times;</span>');

            const $body = $('<div>')
                .addClass('modal-body p-0');

            const $iframe = $('<iframe>')
                .attr({ id: 'popup-frame' })
                .css({
                    width: '100%',
                    border: 0
                });

            $header.append($title, $closeBtn);
            $body.append($iframe);
            $content.append($header, $body);
            $dialog.append($content);
            $popup.append($dialog);

            $('body').append($popup);

            $popup.on('hidden.bs.modal', function () {
                $('#popup-frame').attr('src', 'about:blank');
            });
        }

        // =========================
        // open 할 때마다 설정 적용
        // =========================

        const $dialog = $popup.find('.modal-dialog');
        const $title = $popup.find('.modal-title');
        const $iframe = $popup.find('iframe');

        // size
        $dialog
            .removeClass('modal-sm modal-lg modal-xl modal-fullscreen modal-fullscreen-md')
            .addClass(settings.size);

        // title
        $title.text(settings.title);

        // iframe
        $iframe
            .off('load')
            .on('load', function () {
                this.contentWindow.postMessage(
                    {
                        messageId: settings.messageId,
                        type: 'POPUP_REQUEST',
                        payload: params
                    },
                    '*'
                );
            })
            .css('height', settings.height)
            .attr('src', url);

        $popup.modal('show');
    }

    sendParamToParent(messageId, params) {
        window.parent.postMessage(
            {
                messageId:messageId,
                type: 'POPUP_RESULT',
                payload: params
            },
            '*'
        );
    }

    // 한 화면에서 여러 팝업 결과를 받을 수 있도록 messageId 별로 핸들러를 둔다 (같은 messageId 로 다시 등록하면 교체)
    receiveParam(receiveMessageId, _callback) {
        this._messageHandlers = this._messageHandlers || {};

        if (this._messageHandlers[receiveMessageId]) {
            window.removeEventListener('message', this._messageHandlers[receiveMessageId]);
        }

        const handler = function (event) {
            const {messageId, type, payload } = event.data || {};

            if(receiveMessageId !== messageId) {
                return;
            }

            if (type === 'POPUP_RESULT' || type === 'POPUP_REQUEST') {
                _callback(payload);
            }
        };

        this._messageHandlers[receiveMessageId] = handler;
        window.addEventListener('message', handler);
    }

    close() {
        window.parent.$('#'+this.POPUP_ID).modal('hide');
    }
}

window.App = window.App || {};
window.App.modalPopup = new ModalPopup();