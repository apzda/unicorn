# Apzda Captcha

Now Supports:

1. Image: 图片验证码
2. Slider: 图片滑块验证码
3. Drag: 滑动验证码

## 全局配置

```properties
# 验证码类型,目前支持: image,slider,drag
apzda.cloud.captcha.provider=image
# 测试模式，默认为false. 为true时图片验证码固定为:A12b
apzda.cloud.captcha.props.test-mode=false
# 图片验证码默认宽度，可通过创建接口的参数修改
apzda.cloud.captcha.props.width=105
# 图片验证码默认高度，可通过创建接口的参数修改
apzda.cloud.captcha.props.height=35
# 验证码有效期，默认60分钟
apzda.cloud.captcha.props.timeout=60m
# 校验结果有效期
apzda.cloud.captcha.props.expired=120s
# 校验失败时是否删除，然后重新生成验证码
apzda.cloud.captcha.props.remove-on-invalid=false
# 同一验证码最大校验次数
apzda.cloud.captcha.props.max-try-count=5
# 同一IP地址，一分钟内最多生成验证码次数
apzda.cloud.captcha.props.max-count=60
```

## Image: 图片验证码

```properties
apzda.cloud.captcha.provider=image
# 干扰图形类型，支持: line,shear和circle
apzda.cloud.captcha.props.type=line
# 自定义验证码字符，默认为26个字母（含大写）和10数字
apzda.cloud.captcha.props.codes=
# 验证码长度
apzda.cloud.captcha.props.length=4
# 干扰量
apzda.cloud.captcha.props.count=60
# 在小写敏感，默认为false
apzda.cloud.captcha.props.case-sensitive=false
```

## Slider: 图片滑块验证码

```properties
apzda.cloud.captcha.provider=slider
# 水印,默认无
apzda.cloud.captcha.props.watermark=
# 噪点数量
apzda.cloud.captcha.props.noise=1
# 容忍度
apzda.cloud.captcha.props.tolerant=5
```

## Drag: 滑动验证码

```properties
apzda.cloud.captcha.provider=drag
# 最大乘数(介于1.05于2之间)
apzda.cloud.captcha.props.max-multiple=1.15
```
